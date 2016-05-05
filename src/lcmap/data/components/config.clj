(ns lcmap.data.components.config
  "Initializes configuration values. Merges a potentially nested map
  into component."
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [schema.core :as schema]
            [lcmap.data.config :as config]))

(defrecord Configuration [cfg-opts]
  component/Lifecycle
  (start [component]
    (log/info "Starting configuration component ...")
    (let [cfg-map (config/init cfg-opts)]
      (log/debug cfg-map)
      (merge component cfg-map)))
  (stop [component]
    (log/info "Stopping configuration component ...")))

(defn new-configuration [cfg-opts]
  (log/debug "Building configuration component ...")
  (->Configuration cfg-opts))
