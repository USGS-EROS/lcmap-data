(ns lcmap.data.adopt
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [lcmap.data.espa :as espa]
            [lcmap.data.tile-spec :as tile-spec]))

(defn scene->bands
  "Turn scene at path into a sequence of bands."
  [band-xf path]
  (sequence band-xf (espa/load-metadata path)))

;; Transform functions, names with a "+" prefix should add a new
;; key to the given band.

(defn +path
  "Add absolute path to raster for band."
  [scene band]
  (assoc band :path (.getAbsolutePath (io/file scene (:file_name band)))))

(defn +ubid
  "Derive UBID from other band properties"
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
  "Add tile-spec properties to band. Band must have :file_path key referencing 
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
  (log/debug "Adopting" path "with" opts)
  (let [band-xf (comp (map (partial +path path))
                      (map (partial +opts opts))
                      (map +ubid)
                      (map +spec))]
    (dorun (map #(save db %) (scene->bands band-xf path)))))
