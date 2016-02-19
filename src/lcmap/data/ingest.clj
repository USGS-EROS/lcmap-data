(ns lcmap.data.ingest
  (:require [clojure.core.memoize :as memo]
            [clojure.core.reducers :as r]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojurewerkz.cassaforte.cql :as cql]
            [pandect.algo.md5 :as md5]
            [gdal.core]
            [gdal.dataset]
            [gdal.band]
            [lcmap.data.espa :as espa]
            [lcmap.data.tile-spec :as tile-spec]
            [lcmap.data.util :as util])
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
  "Close the tile's open GDAL dataset"
  [{{data :gdal-data} :band :as tile}]
  (log/debug "Closing GDAL dataset and updating tile data ...")
  (.delete data)
  (assoc-in tile [:band :gdal-data] nil))

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

(defn load-band
  "Load the band data from a GeoTIFF file using GDAL.

  Note that the object that gets created with this operation needs to be
  deleted manually by passing the assoicated tile to the (cleanup) function."
  [band system archive-path]
  (log/debug "Loading band ...")
  (-> band
      (get-gdal-data archive-path)
      (get-tile-spec system)))

(defn get-bands
  "Builds seq of band maps for ESPA archive at path. This will open
  the image referenced by file-name and load the tile spec implied
  by band properties. This seq does *NOT* omit bands that do not
  conform to the tile-spec."
  [system path]
  (log/debug "Building band maps from ESPA parsed metadata")
  (let [bands (espa/load-metadata path)]
    (map #(load-band % system path) bands)))

(defn defined?
  "True if a band has a tile spec and some data. A band without these
  cannot be ingested because it lacks data to ingest or a way to
  align the data to a tiling grid."
  [{spec :tile-spec data :gdal-data :as band}]
  (or (and spec data)
      (log/warn "Band not well defined:" (:file-name band))))

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
        (log/error "Band does not conform:" (:file-name band) checks))))

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

(defn locate-proj-point
  "Build projection coordinate locating function for dataset."
  [dataset]
  (let [[px sx _ py _ sy] (gdal.dataset/get-geo-transform dataset)]
    (log/debug "Build a projection system point finder with" px sx py sy)
    (fn [{x :x y :y :as tile}]
      (log/debug "Locate raster grid x/y" x y "using" px sx py sy)
      (assoc tile
             :tx (long (+ px (* x sx)))
             :ty (long (+ py (* y sy)))))))

(defn get-step
  ""
  [spec]
  (let [step-x (/ (spec :tile-x) (spec :pixel-x))
        step-y (/ (spec :tile-y) (spec :pixel-y))]
    {:step-x step-x :step-y step-y}))

(defn update-tile-data
  "This function is called for every tile that is loaded, associating
  projection and band data with it."
  [tile band framed]
  (log/debug "Updating tile data ...")
  (let [locate-fn (locate-proj-point framed)]
    (-> tile
        locate-fn
        (assoc :band band))))

(defn get-tiles
  "Builds a seq of tile maps. Reprojects band's gdal-data to ensure
  tiles align to the tile spec's grid."
  [system band]
  (log/debug "Building tile seq from band map ..." (get-in band [:tile-spec :ubid]))
  ;; tile-x and tile-y are in terms of projection system units
  ;; and are used along with pixel-x and pixel-y to calculate
  ;; the xstep and ystep.
  (let [{step-x :step-x step-y :step-y} (get-step (:tile-spec band))
        framed (reproject band)
        raster (gdal.dataset/get-band framed 1)
        tiles  (gdal.band/raster-seq
                 raster
                 :xstep (int step-x)
                 :ystep (int step-y))]
    (map #(update-tile-data % band framed) tiles)))

(defn tile->buffer
  "Create seq for tile data given tile type"
  [tile]
  (let [band (tile :band)
        buffer (tile :data)
        data-type (get-in band [:band :tile-spec :data-type])]
    (.order buffer (java.nio.ByteOrder/nativeOrder))
    (.asShortBuffer buffer)))

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
  (log/debug "Checking tile for all fill-data ...")
  (if (nil? (get-in tile [:band :tile-spec :data-fill]))
    (let [buffer (tile->buffer tile)
          filler (band->fill-buffer (tile :band))]
      (log/debug "Compare?" (.compareTo buffer filler))
      (not= 0 (.compareTo buffer filler)))
    ;; We assue that tile specs that lack fill data always
    ;; have some relevant value. This might be a bad idea
    ;; because some bands may have tiles for an acquisition
    ;; moment whereas others will not.
    true))

(defn save
  "Insert data into database"
  [system tile]
  (log/debug "Saving tile data ...")
  (let [{tx :tx ty :ty data :data {acquired :acquired source :source {ubid :ubid} :tile-spec} :band} tile
        conn (get-in system [:database :session])
        table (get-in tile [:band :tile-spec :table-name])]
    (log/debug "Saving" tx ty ubid acquired source)
    (cql/insert-async conn table {:x tx :y ty :ubid ubid :acquired acquired :source source :data data }))
  tile)

(defn reducer-no-op
  ""
  ([] [])
  ([_ _] [])
  ([_ _ _] []))

(defn process-tile
  "Process tile data."
  [system aux-fn tile]
  (log/debug "Procesing tile ...")
  (->> tile
       (aux-fn)
       (save system)
       (cleanup)))

(defn process-band
  "Process band data."
  [system aux-fn band]
  (log/info "Ingesting band:" (get-in band [:tile-spec :ubid]))
  (->> band
       (get-tiles system)
       (r/filter has-data?)
       (r/map #(process-tile system aux-fn %))
       ;(partition (get-in system [:config :opts :batch-size]))
       (r/fold
         (get-in system [:config :opts :batch-size])
         reducer-no-op
         reducer-no-op)
       (r/foldcat)))

(defn process-scene
  "Process scene data."
  [system path]
  (log/info "Ingesting archive:" (.getAbsolutePath path))
  (->> path
       (get-bands system)
       (filter defined?)
       (filter conforms?)))

(defn- -ingest
  "This function does the actual work of ingest."
  [path system aux-fn]
  (->> path
       (process-scene system)
       (map #(process-band system aux-fn %))
       (partition 1)
       (into [])))

(defn write-hash
  "Write a tile's binary md5 hash to a file."
  [tile wtr]
  (log/debug "Writing tile hash to file ...")
  (let [direct-buffer (:data tile)
        _ (.rewind direct-buffer)
        char-data (.toString (.asCharBuffer direct-buffer))]
    (.write wtr (str (md5/md5 char-data) "\n")))
  tile)

(defn ingest-with-hash
  "Set up auxilary function so that each tile will have a hash saved
  to a file."
  [path system]
  (log/info "Preparing to ingest and hash tiles ...")
  (let [file (io/file (get-in system [:config :opts :checksum-outfile]))]
    (with-open [wtr (io/writer file)]
      (-ingest path system (fn [tile] (write-hash tile wtr))))
    (log/info "Saved hash file to" (.getPath file))))

(defn ingest-without-hash
  "Set up an auxilary no-op function (the default case)."
  [path system]
  (-ingest path system identity))

(defn ingest
  "Save raster data at path as tiles."
  [path system]
  (log/info "Will ingest tiles in batches of"
            (get-in system [:config :opts :batch-size]))
  (if (get-in system [:config :opts :checksum-ingest])
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
    (log/debug "Saving tile spec to the database ..." spec)
    (tile-spec/save spec system)))

(defn adopt
  "Save ESPA metadata as tile specs"
  [path system]
  (let [base-spec {:keyspace-name "lcmap"
                   :table-name    "conus"
                   :tile-x        (* 256 30)
                   :tile-y        (* 256 -30)
                   :data-shape    [256 256]}]
    (log/info "Adopting all bands as a tile spec" (.getAbsolutePath path))
    (doseq [band (get-bands system path)
            :let [band-ubid {:ubid (get-ubid band)}
                  band-data (:gdal-data band)
                  data-spec (util/get-spec-from-image band-data)]]
      (save-spec (merge base-spec band-ubid data-spec band) system))))
