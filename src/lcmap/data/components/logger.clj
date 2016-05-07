(ns lcmap.data.components.logger
  (:require [clojure.tools.logging :as log]
            [twig.core :as logger]
            [com.stuartsierra.component :as component]
            [lcmap.data.util :as util]))

(defrecord Logger []
  component/Lifecycle

  (start [component]
    (log/info "Starting logger component ...")
    (let [ns-levels (partition 2 (get-in component [:cfg :logger]))]
      (log/debug "Using log-level" ns-levels)
      (doseq [args ns-levels] (apply logger/set-level! args))
      (log/debug "Logging agent:" log/*logging-agent*)
      (log/debug "Logging factory:" (logger/get-factory))
      (log/debug "Logging factory name:" (logger/get-factory-name))
      (log/debug "Logger:" (logger/get-logger *ns*))
      (log/debug "Logger name:" (logger/get-logger-name *ns*))
      (log/debug "Logger level:" (logger/get-logger-level *ns*))
      (log/debug "Logger context:" (logger/get-logger-context *ns*))
      (log/debug "Logger configurator:" (logger/get-config *ns*))
      (log/debug "Set log level for these namespaces:" ns-levels) ;; XXX just ns?
      (log/debug "Successfully configured logging.")
      component))

  (stop [component]
    (log/info "Stopping logger component ...")
    (log/debug "Component keys" (keys component))
    component))

(defn new-logger []
  (log/debug "Building logger component ...")
  (->Logger))
