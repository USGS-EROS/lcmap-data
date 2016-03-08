(ns lcmap.data.system
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [lcmap.data.components.config :as config]
            [lcmap.data.components.gdal :as gdal]
            [lcmap.data.components.database :as database]
            [lcmap.data.components.logger :as logger]))

(defn build [opts]
  (log/info "Starting system ...")
  (component/system-map
    :config   (component/using
                (config/new-configuration opts)
                [])
    :logger   (component/using
                (logger/new-logger)
                [:config])
    :gdal     (component/using
                (gdal/new-gdal)
                [:config])
    :database (component/using
                (database/new-database)
                [:config])))
