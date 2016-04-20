(ns lcmap.data.components.database
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojure.tools.logging :as log]))

(defrecord Database []
  component/Lifecycle
  (start [component]
    (log/info "Starting DB component ...")
    (let [db-cfg   (-> component :config :db)
          hosts    (:hosts db-cfg)
          opts     (select-keys db-cfg [:port :protocol-version])
          session  (client/connect hosts opts)]
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
  (log/debug "Building DB component ...")
  (->Database))
