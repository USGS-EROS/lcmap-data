(ns lcmap-data-clj.ingest
  (:require [clojure.core.memoize :as memo]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojurewerkz.cassaforte.cql :as cql]
            [pandect.algo.md5 :as md5]
            [gdal.core]
            [gdal.dataset]
            [gdal.band]
            [lcmap-data-clj.espa :as espa]
            [lcmap-data-clj.tile-spec :as tile-spec]
            [lcmap-data-clj.util :as util])
  (:import [java.nio ByteBuffer ShortBuffer CharBuffer]
           [org.gdal.gdal gdal]))

;;; Getting Band Data

(defn get-gdal-data
  "Open GDAL dataset referred to by band file-name"
  [band path]
  ;; TBD: what to do if the file fails to open?
  ;; ...send a pull request so gdal.core/open works with
  ;; a file object?
  (let [file (io/file path (band :file-name))
        path (.getAbsolutePath file)
        dataset (gdal.core/open path)]
    (assoc band :gdal-data dataset)))

(defn cleanup
  "Close an open GDAL dataset"
  [{data :gdal-data :as band}]
  (log/debug "Closing GDAL dataset")
  (.delete data))

(defn get-tile-spec
  "Get tile-spec implied by band's mission, instrument, product, and name"
  [band system]
  (let [params (select-keys band [:mission :instrument :product :band-name])
        specs (tile-spec/find-spec params system)]
    (assoc band :tile-spec (first specs))))

(def get-tile-buffer
  "Create a buffer memoized on data-size and data-fill."
  (clojure.core.memoize/lu
    (fn [data-size data-fill]
      (let [buffer (java.nio.ShortBuffer/allocate data-size)
            backer (.array buffer)]
        (java.util.Arrays/fill backer (short data-fill))
        buffer))))

(defn band-seq
  "Builds seq of band maps for ESPA archive at path. This will open
  the image referenced by file-name and load the tile spec implied
  by band properties. This seq does *NOT* omit bands that do not
  conform to the tile-spec."
  [path system]
  (log/debug "Building band maps from ESPA parsed metadata")
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
      (log/warn "band not well defined" (:file-name band))))

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
        (log/error "band does not conform" (:file-name band) checks))))

;;; Getting Tile Data

(defn get-bounds
  "Find bounding box (in projection coordinate system) of a GDAL dataset"
  [{dataset :gdal-data :as band}]
  (let [[ux sx _ uy _ sy] (gdal.dataset/get-geo-transform dataset)
        px (gdal.dataset/get-x-size dataset)
        py (gdal.dataset/get-y-size dataset)
        lx (+ ux (* sx px))
        ly (+ uy (* sy py))]
    {:w ux :n uy :e lx :s ly :x px :y py}))

(defn get-frame
  "Create a map with new coordinates and dimensions aligned to tile-spec"
  [{spec :tile-spec :as band}]
  (let [{:keys [:n :s :e :w]} (get-bounds band)
        {:keys [:tile-x :tile-y :pixel-x :pixel-y :shift-x :shift-y]} spec
        ;; This is the interval of the grid in projection system units.
        ;; These are the "expanded" coordinates that frame the original dataset.
        west  (+ shift-x (- w (mod w tile-x)))
        north (+ shift-y (- n (mod n tile-y)))
        east  (+ shift-x (- e (mod e (- tile-x))))
        south (+ shift-y (- s (mod s (- tile-y))))
        ;; This is the new width and height in pixels.
        px    (int (Math/abs (/ (- west east) pixel-x)))
        py    (int (Math/abs (/ (- north south) pixel-y)))]
   {:w west :n north :e east :s south :x px :y py}))

(defn reproject
  "Create a new GDAL dataset for band using the band's tile-spec."
  [{spec :tile-spec data :gdal-data :as band}]
  (let [new-bounds (get-frame band)
        driver    (gdal.core/get-driver-by-name "MEM")
        layers    (gdal.dataset/get-band-count data)
        data-type (gdal.band/get-data-type (gdal.dataset/get-band data 1))
        result    (.Create driver "copy" (:x new-bounds) (:y new-bounds) layers data-type)
        layer     (gdal.dataset/get-band result 1)
        fill      (:data-fill spec)
        affine [(:w new-bounds) (:pixel-x spec) 0 (:n new-bounds) 0 (:pixel-y spec)]]
    (log/debug "Reproject using" affine "filled with" (:data-fill spec))
    (.SetGeoTransform result (double-array affine))
    (.SetProjection result (gdal.dataset/get-projection-str data))
    (if fill (.Fill layer (:data-fill spec)))
    (gdal/ReprojectImage data result)
    result))

(defn proj-point-finder
  "Build projection coordinate locating function for dataset"
  [dataset]
  (let [[px sx _ py _ sy] (gdal.dataset/get-geo-transform dataset)]
    (log/debug "Build a projection system point finder with" px sx py sy)
    (fn [{x :x y :y :as tile}]
      (log/debug "locate raster grid x/y" x y "using" px sx py sy)
      (assoc tile
             :tx (long (+ px (* x sx)))
             :ty (long (+ py (* y sy)))))))

(defn get-step
  ""
  [spec]
  (let [step-x (/ (spec :tile-x) (spec :pixel-x))
        step-y (/ (spec :tile-y) (spec :pixel-y))]
    {:step-x step-x :step-y step-y}))

(defn tile-seq
  "Builds a seq of tile maps. Reprojects band's gdal-data to ensure
  tiles align to the tile spec's grid."
  [band system]
  (log/debug "Building tile seq from band map" (-> band :tile-spec :ubid))
  ;; tile-x and tile-y are in terms of projection system units
  ;; and are used along with pixel-x and pixel-y to calculate
  ;; the xstep and ystep.
  (let [{step-x :step-x step-y :step-y} (get-step (:tile-spec band))
        framed (reproject band)
        locate (proj-point-finder framed)
        raster (gdal.dataset/get-band framed 1)]
    (for [tile (gdal.band/raster-seq raster
                                     :xstep (int step-x)
                                     :ystep (int step-y))]
      (-> tile locate (assoc :band band)))))

(defn tile->buffer
  "Create seq for tile data given tile type"
  [tile]
  (let [band (tile :band)
        buffer (tile :data)
        data-type (get-in band [:tile-spec :data-type])]
    (.order buffer (.nativeOrder java.nio.ByteOrder))
    (condp = data-type
      "INT16" (.asShortBuffer buffer)
      "UINT8" (.asCharBuffer buffer))))

(defn band->fill-buffer
  "Get a properly-sized buffer that can be used to quickly determine if a tile
  is composed entirely of fill data."
  [band]
  (let [data-fill (get-in band [:tile-spec :data-fill])
        data-size (* (-> band :tile-spec get-step :step-x)
                     (-> band :tile-spec get-step :step-y))]
    (get-tile-buffer data-size data-fill)))

(defn has-data?
  "Determine if tile's data buffer is more than just fill data."
  [tile]
  (log/debug "Checking tile for all fill-data")
  (if (get-in tile [:tile-spec :data-fill])
    (let [buffer (tile->buffer tile)
          filler (band->fill-buffer (tile :band))]
      (not= 0 (.compareTo buffer filler)))
    ;; We assue that tile specs that lack fill data always
    ;; have some relevant value. This might be a bad idea
    ;; because some bands may have tiles for an acquisition
    ;; moment whereas others will not.
    true))

(defn save
  "Insert data into database"
  [tile system]
  (let [{tx :tx ty :ty data :data {acquired :acquired source :source {ubid :ubid} :tile-spec} :band} tile
        conn (get-in system [:database :session])
        table (get-in tile [:band :tile-spec :table-name])]
    (log/debug "Saving" tx ty ubid acquired source)
    (cql/insert-async conn table {:x tx :y ty :ubid ubid :acquired acquired :source source :data data })))

(defn- -ingest
  "This function does the actual work of ingest."
  [path system aux-fn]
  (log/info "Ingesting archive" (.getAbsolutePath path))
  (doseq [band (filter defined? (band-seq path system))
          :when (conforms? band)]
    (log/info "Ingesting band" (get-in band [:tile-spec :ubid]))
    (doseq [tile (tile-seq band system)
            :when (has-data? tile)]
      (aux-fn tile)
      (save tile system))
    ;; XXX Find a way to open and close a band's dataset
    ;; in the same context.
    (cleanup band)))

(defn ingest-with-hash
  "Set up auxilary function so that each tile will have a hash saved
  to a file."
  [path system]
  (log/info "Preparing to ingest and hash tiles ...")
  (let [dir (System/getProperty "java.io.tmpdir")
        filename (str dir "/ingest-hashes.txt")]
    (with-open [wtr (io/writer filename)]
      (-ingest path system (fn [data]
                             (let [direct-buffer (:data data)
                                   _ (.rewind direct-buffer)
                                   char-data (.toString (.asCharBuffer direct-buffer))]
                               (.write wtr (str (md5/md5 char-data) "\n"))))))
    (log/info "Saved hash file to" filename)))

(defn ingest-without-hash
  "Set up an auxilary no-op function (the default case)."
  [path system]
  (-ingest path system identity))

(defn ingest
  "Save raster data at path as tiles."
  [path system & {:keys [do-hash?]}]
  (if do-hash?
    (ingest-with-hash path system)
    (ingest-without-hash path system)))

(defn get-ubid [band]
  (apply str (interpose "/" ((juxt :satellite :instrument :band-name) band))))

(defn save-spec
  [band system]
  (let [spec (select-keys band [:keyspace-name :table-name
                                :projection
                                :tile-x :tile-y :pixel-x :pixel-y
                                :shift-x :shift-y
                                :satellite :instrument :ubid
                                :band-name :band-short-name :band-long-name
                                :band-product :band-category
                                :data-fill :data-range :data-scale :data-type
                                :data-units :data-mask :data-shape])]
    (log/debug "Saving tile spec" spec)
    (tile-spec/save spec system)))

(defn adopt
  "Save ESPA metadata as tile specs"
  [path system]
  (let [base-spec {:keyspace-name "lcmap"
                   :table-name    "conus"
                   :tile-x        (* 256 30)
                   :tile-y        (* 256 -30)
                   :data-shape    [256 256]}]
    (log/debug   "Adopting all bands as a tile spec" (.getAbsolutePath path))
    (doseq [band (band-seq path system)
            :let [band-ubid {:ubid (get-ubid band)}
                  band-data (:gdal-data band)
                  data-spec (util/get-spec-from-image band-data)]]
      (save-spec (merge base-spec band-ubid data-spec band) system))))
