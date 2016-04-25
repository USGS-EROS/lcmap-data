(ns lcmap.data.adopt
  "Functions supporting the creation of tile-specs from ESPA XML metadata
   and associated rasters (e.g. GDAL datasets)."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lcmap.data.espa :as espa]
            [lcmap.data.tile-spec :as tile-spec]))

(defn scene->bands
  "Turn scene at path into a sequence of bands."
  [band-xf path]
  (sequence band-xf (espa/load path)))

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

(defn process-scene
  "Produce tile-specs for all bands in scene"
  [db path opts]
  (log/info "Adopting" path "with" opts)
  (let [band-xf (comp (map (partial +opts opts))
                      (map +spec))]
    ;; dorun is needed to realize the sequence
    (dorun (map #(tile-spec/save db %) (scene->bands band-xf path)))))
