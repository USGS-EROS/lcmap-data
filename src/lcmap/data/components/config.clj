(ns lcmap.data.components.config
  "Config contains environment, profile, and command line parameter values."
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]))

(defrecord Configuration [db logger opts]
  component/Lifecycle
  (start [component]
    (log/info "Starting configuration component ...")
    (assoc component :started true))
  (stop [component]
    (log/info "Stopping configuration component ...")
    (dissoc component :db :logger :opts :started)))

(defn new-configuration [{db :db logger :logger opts :opts :as config}]
  (log/debug "Building configuration component ...")
  (->Configuration db logger opts))
