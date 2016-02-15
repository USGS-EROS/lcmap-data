(ns lcmap-data-clj.components.database
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.policies :as policies]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojure.tools.logging :as log]))

(defrecord Database []
  component/Lifecycle
  (start [component]
    (log/info "Starting DB component ...")
    (let [db-cfg   (-> component :config :db)
          hosts    (:hosts db-cfg)
          opts     (dissoc db-cfg :hosts)
          session  (client/connect hosts opts)
          keyspace (:spec-keyspace db-cfg)]
      (try
        (cql/use-keyspace session keyspace)
        (catch Exception ex
          (log/warn "Could not use keyspace")))
      (policies/constant-reconnection-policy 250 #_ms)
      (policies/retry-policy :default)
      (policies/with-consistency-level :any)
      (assoc component :session session)))
  (stop [component]
    (log/info "Stopping DB component ...")
    (try
      (let [session (-> component :session)]
        (client/disconnect session))
      (catch Exception ex
        (log/error "Could not disconnect from session")))
    (dissoc component :session)))

(defn new-database []
  (log/info "Building DB component ...")
  (->Database))
