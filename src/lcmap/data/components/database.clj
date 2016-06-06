(ns lcmap.data.components.database
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.cassaforte.client :as client]
            [com.stuartsierra.component :as component]
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
    (try
      (let [session (get-in component [:session])]
        (client/disconnect session))
      (catch Exception ex
        (log/error "Could not disconnect from session")))
    (dissoc component :session)))

(defn new-database []
  (log/debug "Building DB component ...")
  (->Database))
