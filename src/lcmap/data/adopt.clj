(ns lcmap.data.adopt
  "Functions supporting the creation of tile-specs from ESPA XML metadata
   and associated rasters (e.g. GDAL datasets)."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lcmap.data.espa :as espa]
            [lcmap.data.tile-spec :as tile-spec]))

;; These functions are implemented in their own namespace instead of
;; lcmap.data.tile-spec because they bridge the gap between ESPA XML
;; metadata and tile-specs.

(defn scene->bands
  "Turn scene at path into a sequence of bands."
  [band-xf path]
  (sequence band-xf (espa/load-metadata path)))

;; Transform functions have a "+" prefix to imply that they assoc a
;; value with the last argument. These functions are intended to be
;; used with transducers; composed together to transform (or filter)
;; a set of bands into tile-specs.

(defn +path
  "Add absolute path to raster for band. ESPA XML contains a relative
   path to the raster."
  [scene band]
  (assoc band :path (.getAbsolutePath (io/file scene (:file_name band)))))

(defn +ubid
  "Derive UBID from other band properties."
  [band]
  (let [vs ((juxt :satellite :instrument :band_name) band)]
    (assoc band :ubid (clojure.string/join "/" vs))))

(defn +opts
  "Add user specified options to band. This cannot be inferred from 
  ESPA metadata, usually provided as a set of CLI or ENV options."
  [opts band]
  (let [{:keys [tile-size tile-keyspace tile-table]} opts]
    (assoc band
           :data_shape tile-size
           :keyspace_name tile-keyspace
           :table_name tile-table)))

(defn +spec
  "Add tile-spec properties to band. Band must have :path key referencing 
  dataset."
  [band]
  (merge band (tile-spec/dataset->spec (:path band)
                                       (:data_shape band))))

(defn save
  "Save band as tile-spec."
  [db band]
  (tile-spec/save db band))

(defn process-scene
  "Produce tile-specs for all bands in scene"
  [db path opts]
  (log/info "Adopting" path "with" opts)
  (let [band-xf (comp (map (partial +path path))
                      (map (partial +opts opts))
                      (map +ubid)
                      (map +spec))]
    ;; dorun is needed to realize the sequence
    (dorun (map #(save db %) (scene->bands band-xf path)))))
