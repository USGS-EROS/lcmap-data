(ns lcmap-data-clj.ingest
  (:require [lcmap-data-clj.util :as util]
            [lcmap-data-clj.espa :as espa]
            [lcmap-data-clj.tile-spec :as tile-spec]
            [clj-gdal.core :as gc]
            [clj-gdal.dataset :as gd]
            [clj-gdal.band :as gb]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojurewerkz.cassaforte.cql :as cql])
  (:import [java.nio ByteBuffer ShortBuffer CharBuffer]
           [org.gdal.gdal gdal]))

;;; Getting Band Data

(defn get-gdal-data
  "Open GDAL dataset referred to by band file-name"
  [band path]
  ;; TBD: what to do if the file fails to open?
  ;; ...send a pull request so gc/open works with
  ;; a file object?
  (let [file (io/file path (band :file-name))
        path (. file getAbsolutePath)
        dataset (gc/open path)]
    (assoc band :gdal-data dataset)))

(defn get-tile-spec
  "Get tile-spec implied by band's mission, instrument, product, and name"
  [band system]
  (let [params (select-keys band [:mission :instrument :product :band-name])
        specs (tile-spec/find params system)]
    (assoc band :tile-spec (first specs))))

(defn band-seq
  "Builds seq of band maps for ESPA archive at path. This will open
  the image referenced by file-name and load the tile spec implied
  by band properties. This seq does *NOT* omit bands that do not
  conform to the tile-spec."
  [path system]
  (log/info "Building band maps from ESPA parsed metadata")
  (let [bands (espa/load-metadata path)]
    (for [band bands]
      (-> band
          (get-gdal-data path)
          (get-tile-spec system)))))

(defn defined?
  "True if a band has a tile spec and some data. A band without these
  cannot be ingested because it lacks data to ingest or a way to
  align the data to a tiling grid."
  [{spec :tile-spec data :gdal-data :as band}]
  (or (and spec data)
      (log/info "band not well defined" (:file-name band))))

(defn conforms?
  "Determine if a band's GDAL dataset is compatible with tile-spec."
  [{spec :tile-spec data :gdal-data :as band}]
  (log/debug "Checking image:" data)
  (let [data-spec  (util/get-spec-from-image data)
        data-proj  (util/get-proj-from-spec data-spec)
        spec-proj  (util/get-proj-from-spec spec)
        checks     {:proj (util/equiv spec-proj data-proj)
                    :pixel-x (== (data-spec :pixel-x) (spec :pixel-x))
                    :pixel-y (== (data-spec :pixel-y) (spec :pixel-y))
                    :shift-x (== (data-spec :shift-x) (spec :shift-x))
                    :shift-y (== (data-spec :shift-y) (spec :shift-y))}]
    (log/debug "Conformance check results" checks)
    (or (every? true? (vals checks))
        (log/info "band does not conform" (:file-name band) checks))))

;;; Getting Tile Data

(defn get-bounds
  "Find bounding box (in projection coordinate system) of a GDAL dataset"
  [{dataset :gdal-data :as band}]
  (let [[ux sx _ uy _ sy] (gd/get-geo-transform dataset)
        px (gd/get-raster-x-size dataset)
        py (gd/get-raster-y-size dataset)
        lx (+ ux (* sx px))
        ly (+ uy (* sy py))]
    {:w ux :n uy :e lx :s ly :x px :y py}))

(defn get-frame
  "Create a map with new coordinates and dimensions aligned to tile-spec"
  [{spec :tile-spec :as band}]
  (let [{:keys [:n :s :e :w]} (get-bounds band)
        {:keys [:tile-x :tile-y :pixel-x :pixel-y :shift-x :shift-y]} spec
        ;; This is the interval of the grid in projection system units.
        grid-x (* tile-x pixel-x)
        grid-y (* tile-y pixel-y)
        ;; These are the "expanded" coordinates that frame the original dataset.
        west  (+ shift-x (- w (mod w grid-x)))
        north (+ shift-y (- n (mod n grid-y)))
        east  (+ shift-x (- e (mod e (- grid-x))))
        south (+ shift-y (- s (mod s (- grid-y))))
        ;; This is the new width and height in pixels.
        px    (int (Math/abs (/ (- west east) pixel-x)))
        py    (int (Math/abs (/ (- north south) pixel-y)))]
   {:w west :n north :e east :s south :x px :y py}))

(defn reproject
  "Create a new GDAL dataset for band using the band's tile-spec."
  [{spec :tile-spec data :gdal-data :as band}]
  (let [new-bounds (get-frame band)
        driver    (gc/get-driver-by-name "MEM")
        layers    (gd/get-raster-count data)
        data-type (gb/get-data-type (gd/get-raster-band data 1))
        result    (. driver Create "copy" (:x new-bounds) (:y new-bounds) layers data-type)
        layer     (gd/get-raster-band result 1)
        affine [(:w new-bounds) (:pixel-x spec) 0 (:n new-bounds) 0 (:pixel-y spec)]]
    (. result SetGeoTransform (double-array affine))
    (. result SetProjection (gd/get-projection data))
    (. layer Fill (:data-fill spec))
    (gdal/ReprojectImage data result)
    result))

(defn proj-point-finder
  "Build projection coordinate locating function for dataset"
  [dataset]
  (let [[px sx _ py _ sy] (gd/get-geo-transform dataset)]
    (fn [{x :x y :y :as tile}]
      (assoc tile
             :tx (long (+ px (* x sx)))
             :ty (long (+ py (* y sy)))))))

(defn tile-seq
  "Builds a seq of tile maps. Reprojects band's gdal-data to ensure
  tiles align to the tile spec's grid."
  [band system]
  (log/debug "Building tile seq from band map")
  (let [{{xstep :tile-x ystep :tile-y} :tile-spec} band
        framed (reproject band)
        locate (proj-point-finder framed)
        raster (gd/get-raster-band framed 1)]
    (for [tile (gb/raster-seq raster :xstep xstep :ystep ystep)]
      (-> tile locate (assoc :band band)))))

(defn tile->buffer
  "Create seq for tile data given tile type"
  [tile]
  (let [band (tile :band)
        buffer (tile :data)
        data-type (-> band :tile-spec :data-type)]
    (. buffer order (. java.nio.ByteOrder nativeOrder))
    (condp = data-type
      "INT16" (. buffer asShortBuffer)
      "UINT8" (. buffer asCharBuffer))))

(defn band->fill-buffer
  "Create a buffer that can be used to quickly determine if a tile is
  composed entirely of fill data."
  [band]
  (let [data-type (-> band :tile-spec :data-type)
        data-fill (-> band :tile-spec :data-fill)
        data-size (* (-> band :tile-spec :tile-x)
                     (-> band :tile-spec :tile-y))]
    (condp = data-type
      "INT16" (let [buffer (java.nio.ShortBuffer/allocate data-size)
                    backer (. buffer array)]
                (java.util.Arrays/fill backer (short data-fill))
                buffer)
      "UINT8" (let [buffer (java.nio.CharBuffer/allocate data-size)
                    backer (. buffer array)]
                (java.util.Arrays/fill backer (char data-fill))
                buffer))))

(defn has-data?
  "Determin if tile's data buffer is more than just fill data."
  [tile]
  (log/debug "Checking tile for all fill-data")
  (let [buffer (tile->buffer tile)
        filler (band->fill-buffer (tile :band))]
    (not= 0 (. buffer compareTo filler))))

(defn save
  "Insert data into database"
  [tile system]
  (let [{tx :tx ty :ty data :data {acquired :acquired {ubid :ubid} :tile-spec} :band} tile
        conn (-> system :database :session)
        table (-> tile :band :tile-spec :table-name)]
    (cql/insert conn table {:x tx :y ty :ubid ubid :acquired acquired :data data })
    (log/info "Saving " tx ty ubid acquired)))

(defn ingest
  "Save raster data at path as tiles."
  [path system]
  (doseq [band (filter defined? (band-seq path system))
          :when (conforms? band)]
    (doseq [tile (tile-seq band system)
            :when (has-data? tile)]
      (save tile system))))

(defn get-ubid [band]
  (apply str (interpose "/" ((juxt :satellite :instrument :band-name) band))))

(defn save-spec
  [band system]
  (let [spec (select-keys band [:keyspace-name :table-name :satellite :instrument :ubid :projection
                                :tile-x :tile-y :pixel-x :pixel-y :shift-x :shift-y
                                :band-name :band-short-name :band-long-name :band-product :band-category
                                :data-fill :data-range :data-scale :data-type :data-units :data-mask])]
    (log/info "Saving tile spec" (:ubid spec))
    (try
      (tile-spec/save spec system)
      (catch Exception ex
        (println ex)
        (log/error ex)))))

(defn adopt
  "Save ESPA metadata as tile specs"
  [path system]
  (let [base-spec {:keyspace-name "lcmap"
                   :table-name    "conus"
                   :tile-x        256
                   :tile-y        256}]
    (log/info "Adopting all bands as a tile spec" path)
    (doseq [band (band-seq path system)
            :let [band-ubid {:ubid (get-ubid band)}
                  band-data (:gdal-data band)
                  data-spec (util/get-spec-from-image band-data)]]
      (save-spec (merge base-spec band-ubid data-spec band) system))))
