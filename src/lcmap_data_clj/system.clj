(ns lcmap-data-clj.system
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [lcmap-data-clj.components.config :as config]
            [lcmap-data-clj.components.gdal :as gdal]
            [lcmap-data-clj.components.database :as database]
            [lcmap-data-clj.components.logger :as logger]))

(defn build [opts]
  (log/info "Building system components map")
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
