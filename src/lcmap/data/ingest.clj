(ns lcmap.data.ingest
  "Funtions related to the creation of tiles from ESPA XML metadata and
   associated rasters (e.g. GDAL datasets)"
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.core.memoize]
            [clojure.set]
            [lcmap.data.espa :as espa]
            [lcmap.data.scene :as scene]
            [lcmap.data.tile :as tile]
            [lcmap.data.tile-spec :as tile-spec]
            [dire.core :refer [with-handler!]]
            [gdal.core]
            [gdal.dataset]
            [pandect.algo.md5 :as md5]))

(defn +spec
  "Retrieve a spec (for the given UBID) and add it to the band. This assumes
   only one tile-spec will be found. If multiple tile-specs exists, behavior
   is undefined."
  ;; XXX In the future, we will support multiple areas of interest, regions
  ;;     that are projected differently (CONUS, Alaska, Hawaii). Once we get
  ;;     to that point, we will use the scene's projection as an additional
  ;;     parameter to select the tile-spec.
  [db band]
  (merge band (first (tile-spec/find db (select-keys band [= :ubid (:ubid band)])))))

(defn- int16-fill
  "Produce a buffer used to detect INT16 type buffers containing all fill data."
  [data-size data-fill]
  (let [buffer (java.nio.ShortBuffer/allocate data-size)
        backer (.array buffer)]
    (java.util.Arrays/fill backer (short data-fill))))

(defn- uint8-fill
  "PLACEHOLDER. Produce a buffer used to detect UINT8 type buffers all fill data."
  [data-size data-fill]
  ;; XXX This hasn't been implemented yet because it's not strictly
  ;;     necessary at this point. If a band doesn't have a fill buffer
  ;;     to compare against, it assumes the tile has useful data. The
  ;;     downside is we are creating tiles comprised entirely of fill
  ;;     data.
  nil)

(def fill-buffer
  "Create a buffer memoized on data-size and data-fill."
  (clojure.core.memoize/lu
   (fn [data-size data-fill data-type]
     (log/debug "Create fill buffer for" data-size data-fill data-type)
     (cond (= data-type "INT16") (int16-fill data-size data-fill)
           (= data-type "UINT8") (uint8-fill data-size data-fill)))))

(defn +fill
  "Make a fill buffer used to detect no-data tiles"
  [band]
  (log/debug "add fill buffer to band ...")
  (assoc band :fill (fill-buffer (apply * (band :data_shape))
                                 (band :data_fill)
                                 (band :data_type))))

(defn fill?
  "True if the tile is comprised entirely of fill values"
  [tile]
  (let [data (:data tile)
        fill (:fill tile)]
    (log/debug "checking fill ...")
    (and (some? fill)
         (some? data)
         (= 0 (.compareTo data fill)))))

(defn locate-fn
  "Build projection coordinate point calculator for GDAL dataset."
  [band]
  (log/debug "creating locate fn for band ...")
  ;; XXX It's possible to use a GDAL transform function to obtain
  ;;     the projection system coordinates for a pixel instead of
  ;;     doing the arithmetic ourselves. However, this approach is
  ;;     simple-enough for now.
  (gdal.core/with-dataset [dataset (band :path)]
    (let [[px sx _ py _ sy] (gdal.dataset/get-geo-transform dataset)]
      (fn [{x :x y :y :as tile}]
        (let [tx (long (+ px (* x sx)))
              ty (long (+ py (* y sy)))]
          (assoc tile :proj-x tx :proj-y ty))))))

(defn +locate
  "Make a raster to projection point transformer function."
  [band]
  (assoc band :locate-fn (locate-fn band)))

(defn locate
  "Use band's locator to turn a raster point to projection point."
  [tile]
  ((:locate-fn tile) tile))

(defn conforms?
  "PLACHOLER. True if the referenced raster matches the band's tile-spec.
   This ensures the raster is the same projection and that the boundaries
   precisely align to the tile-specs grid values."
  [band]
  ;; XXX This is a safety net that we don't need at the moment;
  ;;     we can guarantee the dimensions of ESPA outputs. In order
  ;;     to make ingest reusable by other people, teams, etc...
  ;;     it would be nice to prevent data that doesn't match the
  ;;     tile-spec from being ingested.
  true)

(defn get-hash
  "Calculate MD5 checksum of tile"
  [tile]
  (let [direct-buffer (:data tile)
        _ (.rewind direct-buffer)
        char-data (str (.asCharBuffer direct-buffer))]
    (str (md5/md5 char-data))))

(defn checksum
  "Produce a checksum for tile data. Useful for building a secondary inventory
   of ingested data and verifying multiple runs produce the same tile data.
   This relies on configuring underlying logging (currently); see resources/logback.xml
   for details."
  [tile]
  (log/debug "checksum" (:ubid tile) (:proj-x tile) (:proj-y tile) (:acquired tile) (:source tile) (get-hash tile))
  tile)

(defn save
  "Save a tile. This function should be used for all saving that needs
   to happen (in batch) when processing a tile. Currently, this only
   inserts tile data, but it will soon update a scene inventory too."
  [db tile]
  (let [params (-> tile
                   (select-keys [:ubid :proj-x :proj-y :acquired :source :data])
                   (clojure.set/rename-keys {:proj-x :x :proj-y :y}))
        keyspace (tile :keyspace_name)
        table (tile :table_name)]
    (log/debug "saving tile ..." params)
    (tile/save db keyspace table params)))

;;; tile producing functions

(defn scene->bands
  "Create sequence of from ESPA XML metadata."
  [path band-xf]
  (log/debug "producing bands for scene ...")
  (sequence band-xf (espa/load path)))

(defn dataset->tiles
  "Create sequence of tile from dataset referenced by band."
  [tile-xf dataset x-step y-step]
  (log/debug "producing tiles for dataset ...")
  (let [image (gdal.dataset/get-band dataset 1)
        tiles (tile/tiles image x-step y-step)]
    (sequence tile-xf tiles)))

;; processing functions

(defn process-tile
  "Isolates all side-effect related behavior performed on each tile"
  [db tile]
  (log/debug "processing tile ...")
  (io!
   (checksum tile)
   (save db tile))
  tile)

(defn process-band
  "Saves band as tiles."
  [db band]
  ;; Tiles must be saved within the context of dataset or else the
  ;; data buffer will reference a byte buffer that cannot be read!
  (gdal.core/with-dataset [dataset (:path band)]
    (let [tile-xf (comp (map #(merge band %))
                        (map locate)
                        (filter fill?))
          [xs ys] (:data_shape band)
          tiles   (dataset->tiles tile-xf dataset xs ys)]
      (log/info "processing band started ..." (:ubid band))
      (scene/save-band db band)
      (dorun (pmap #(process-tile db %) tiles))
      (log/info "processing band done ..." (:ubid band)))))

(defn process-scene
  "Saves all bands in dir referenced by path."
  [db scene-dir]
  (let [band-xf (comp (map (partial +spec db))
                      (map +fill)
                      (map +locate)
                      (filter conforms?))]
    (log/info "start processing scene" scene-dir)
    (dorun (pmap #(process-band db %) (scene->bands scene-dir band-xf)))
    (log/info "done processing" scene-dir)))

;;; Exception handling

(with-handler! #'process-tile
  java.lang.Exception
  (fn [e & [db tile]]
    (log/error "Failed to process tile" tile)))

(with-handler! #'dataset->tiles
  java.lang.Exception
  (fn [e & [db band]]
    (log/error "Failed to turn dataset into tiles" e)))
