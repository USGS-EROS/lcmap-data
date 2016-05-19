(ns lcmap.data.system
  "Ingest requires a handful of components in order to produce tiles and
   tile-specs.

   - config holds environment, profile, and command line options.
   - logger enables and adjusts log levels.
   - gdal provides access to raster data.
   - database maintains a session used to store and retrieve tiles
     and tile-specs.
  "
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [lcmap.config.components.config :as config]
            [lcmap.data.components.gdal :as gdal]
            [lcmap.data.components.database :as database]
            [lcmap.data.config]
            [lcmap.logger.components.logger :as logger]))

(defn build
  ([]
    (build lcmap.data.config/defaults))
  ([opts]
    (log/info "Starting system ...")
    (component/system-map
      :cfg     (component/using
                 (config/new-configuration opts)
                 [])
      :logger   (component/using
                  (logger/new-logger)
                  [:cfg])
      :gdal     (component/using
                  (gdal/new-gdal)
                  [:cfg])
      :database (component/using
                  (database/new-database)
                  [:cfg]))))

(defn stop [system component-key]
  (->> system
       (component-key)
       (component/stop)
       (assoc system component-key)))

(defn start [system component-key]
  (->> system
       (component-key)
       (component/start)
       (assoc system component-key)))

(defn restart [system component-key]
  (-> system
      (stop component-key)
      (start component-key)))

