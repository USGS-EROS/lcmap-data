(ns lcmap-data-clj.components.gdal
  (:require [com.stuartsierra.component :as component]
            [gdal.core :as gc]
            [clojure.tools.logging :as log]))
(defrecord GDAL []

  component/Lifecycle
  (start [component]
    (log/debug "start gdal component")
    (gc/init)
    component)
  (stop [component]
    (log/debug "stop gdal component")
    component))

(defn new-gdal []
  (log/debug "build component GDAL")
  (->GDAL))
