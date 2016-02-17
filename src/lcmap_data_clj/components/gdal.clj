(ns lcmap-data-clj.components.gdal
  (:require [com.stuartsierra.component :as component]
            [gdal.core :as gc]
            [clojure.tools.logging :as log]))
(defrecord GDAL []

  component/Lifecycle
  (start [component]
    (log/info "Starting GDAL component ...")
    (gc/init)
    component)
  (stop [component]
    (log/info "Stopping GDAL component ...")
    component))

(defn new-gdal []
  (log/debug "Building GDAL component ...")
  (->GDAL))
