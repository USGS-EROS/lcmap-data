(ns lcmap.data.components.database
  ""
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.cassaforte.client :as client]
            [com.stuartsierra.component :as component]
            [dire.core :refer [with-handler!]]
            [lcmap.config.cassaforte :refer [connect-opts]]))

(defrecord Database []
  component/Lifecycle
  (start [component]
    (log/info "Starting DB component ...")
    (let [db-conf (get-in component [:cfg :lcmap.data])
          session (apply client/connect (connect-opts db-conf))]
      (assoc component :session session)))
  (stop [component]
    (log/info "Stopping DB component ...")
    (if-let [session (get-in component [:session])]
      (client/disconnect session)
      (log/info "No DB session to disconnect ..."))
    (dissoc component :session)))

(defn new-database []
  (log/debug "Building DB component ...")
  (->Database))

(with-handler! #'client/connect
  com.datastax.driver.core.exceptions.NoHostAvailableException
  (fn [e & [component]]
    (log/error "No Cassandra cluster available.")
    component))

(with-handler! #'client/disconnect
  java.lang.RuntimeException
  (fn [e & [component]]
    (log/error "Could not disconnect from session.")
    component))
