(ns lcmap.data.content
  (:require [gdal.band :as band]
            [gdal.core :as gdal]
            [gdal.dataset :as dataset]
            [gdal.driver :as driver]
            [lcmap.data.tile :as tile]
            [lcmap.data.tile-spec :as tile-spec])
  (:import [org.gdal.gdalconst gdalconst]))

;; create an array buffer for the dataset
;; create a virtual file backed by the buffer
;; flush dataset
;; create ByteArrayOutputStream

(def meow (cycle (range -128 128)))

(def moof (into-array Byte/TYPE (take (* 256 256) meow)))

(defn create
  "Icky-Icky-Icky-Ptang-Zoop-Boing"
  [driver-name tile-spec tiles]
  (println "party time!")
  (let [le-driver   (gdal/get-driver-by-name driver-name)
        le-path     (str "neato." (driver/file-ext le-driver))
        le-dataset  (driver/create le-driver le-path 128 128 (count tiles) gdalconst/GDT_Int16)
        le-tile     (first tiles)
        le-array    (short-array (* 128 128))]
    ;; set metadata using tile-spec and tile properties...
    (dataset/set-projection-str le-dataset (:projection tile-spec))
    (dataset/set-geo-transform le-dataset [(:x le-tile) 30.0 0.0 (:y le-tile) 0.0 -30.0])
    (doseq [tile tiles]
      (let [le-band (dataset/get-band le-dataset 1)]
        (-> tile :data (.asShortBuffer) (.get le-array))
        (band/write-raster le-band 0 0 128 128 le-array)
        (band/flush-cache le-band)
        (println "ok, wrote a band apparently")))
    (dataset/flush-cache le-dataset)
    (dataset/delete le-dataset) ;; mandatory
    le-dataset))
