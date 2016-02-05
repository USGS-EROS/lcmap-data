(ns lcmap-data-clj.components.config
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]))

(defrecord Configuration [db logger]
  component/Lifecycle
  (start [component]
    (log/info "Start configuration component ...")
    (assoc component :started true))
  (stop [component]
    (log/info "Stopping configuration component ...")
    (dissoc component :db :logger :started)))

(defn new-configuration [{db :db logger :logger :as config}]
  (log/info "Building configuration component ...")
  (->Configuration db logger))
