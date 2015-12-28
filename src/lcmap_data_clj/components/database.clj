(ns lcmap-data-clj.components.database
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojure.tools.logging :as log]))

(defrecord Database []
  component/Lifecycle
  (start [component]
    (log/debug "start component database")
    (let [hosts    (-> component :config :db :hosts)
          keyspace (-> component :config :db :spec-keyspace)
          session  (client/connect hosts)]
      ;; It's possible that the keyspace does not exist.
      (try
        (cql/use-keyspace session keyspace)
        (catch Exception ex
          (log/error "Could not use keyspace" (ex-data ex))
          component))
      (assoc component :session session)))
  (stop [component]
    (log/debug "stop component database")
    (try
      (let [session (-> component :session)]
        (client/disconnect session))
      (catch Exception ex
        (log/error "Could not disconnect from session")))
    (dissoc component :session)))

(defn new-database []
  (log/debug "build component database")
  (->Database))
