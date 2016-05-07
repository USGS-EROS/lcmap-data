(ns lcmap.data.components.database
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojure.tools.logging :as log]))

(defrecord Database []
  component/Lifecycle
  (start [component]
    (log/info "Starting DB component ...")
    (let [db-conf (get-in component [:cfg :lcmap.data.components.db])
          hosts   (:hosts db-conf)
          opts    {} ;; XXX revisit (user, pass, policies...)
          session (client/connect hosts opts)]
      (-> component
          (assoc :session session)
          (merge db-conf))))
  (stop [component]
    (log/info "Stopping DB component ...")
    (try
      (let [session (component :session)]
        (client/disconnect session))
      (catch Exception ex
        (log/error "Could not disconnect from session")))
    (dissoc component :session)))

(defn new-database []
  (log/debug "Building DB component ...")
  (->Database))
