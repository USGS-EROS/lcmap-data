;;;; LCMAP Data development namespace
;;;;
;;;; This namespace simplifies active development by providing easier
;;;; ways to init, start, stop system components.
(ns lcmap.data.dev
  (:require [lcmap.data.system :as system]
            [lcmap.data.ingest :as ingest]
            [lcmap.data.tile-spec :as tile-spec]
            [lcmap.data.util :as util]
            [lcmap.data.espa  :as espa]
            [gdal.core :as gc]
            [gdal.dataset :as gd]
            [gdal.band :as gb]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.query :as cq]
            [clojurewerkz.cassaforte.utils :as cu]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojure.tools.logging :as log]
            [twig.core :as logger]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [com.stuartsierra.component :as component]
            [leiningen.core.project :as lein-prj]))

(def the-system nil)

(defn init
  "Prepare the system without starting it"
  []
  (alter-var-root #'the-system #(when-not % (system/build (util/get-config)))))

(defn start
  "Start the system (if it exists)"
  []
  (alter-var-root #'the-system #(when % (component/start %))))

(defn stop
  "Stop the system (if it exists)"
  []
  (alter-var-root #'the-system #(when % (component/stop %))))

(defn deinit
  "Forget the system map"
  []
  (alter-var-root #'the-system (fn [_] nil)))

(defn run
  "init -> start"
  []
  (init)
  (start))

(defn reset
  "stop -> init -> start -> refresh repl"
  []
  (stop)
  (deinit)
  (refresh :after 'lcmap.data.dev/run))

(def reload #'reset)
