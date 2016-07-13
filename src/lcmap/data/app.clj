(ns lcmap.data.app
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clojusc.twig :as logger]
            [lcmap.data.system :as components]
            [lcmap.data.util :as util])
  (:gen-class))

(defn -main
  "This is the entry point. Note, however, that the system components are
  defined in lcmap.data.system.

  'lein run' will use this as well as 'java -jar'."
  [& args]
  ;; Set the initial log-level before the components set the log-levels for
  ;; the configured namespaces
  (logger/set-level! ['lcmap] :info)
  (let [system (components/build)
        local-ip  (.getHostAddress (java.net.InetAddress/getLocalHost))]
    (log/info "LCMAP data service's local IP address:" local-ip)
    (component/start system)
    (log/info "LCMAP Data startup complete.")
    (util/add-shutdown-handler #(component/stop system))))
