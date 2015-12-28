(ns lcmap-data-clj.components.config
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]))

(defrecord Configuration [db log]
  component/Lifecycle
  (start [component]
    (log/debug "start component configuration")
    (assoc component :started true))
  (stop [component]
    (log/debug "stop component configuration")
    (dissoc component :db :log :started)))

(defn new-configuration [{db :db log :log :as config}]
  (log/debug "build component configuration")
  (->Configuration db log))
