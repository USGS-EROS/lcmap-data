(ns lcmap.data.dev
  ""
  (:require [lcmap.data.config :as config]
            [lcmap.data.system :as system]
            [lcmap.data.ingest :as ingest]
            [lcmap.data.tile :as tile]
            [lcmap.data.tile-spec :as spec]
            [lcmap.data.scene :as scene]
            [lcmap.data.util :as util]
            [lcmap.data.espa  :as espa]
            [clojure.tools.namespace.repl :as repl]
            [com.stuartsierra.component :as component]))

(def sys nil)

(def cfg config/defaults)

(defn init
  "Prepare the system without starting it"
  []
  (alter-var-root #'sys #(when-not % (system/build cfg))))

(defn start
  "Start the system (if it exists)"
  []
  (alter-var-root #'sys #(when % (component/start %))))

(defn stop
  "Stop the system (if it exists)"
  []
  (alter-var-root #'sys #(when % (component/stop %))))

(defn deinit
  "Forget the system map"
  []
  (alter-var-root #'sys (fn [_] nil)))

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
  (repl/refresh :after 'lcmap.data.dev/run))

(def reload #'reset)


(comment "Some basic usage examples."

  ;; Starting the system
  (run)

  ;; Getting some specs
  (let [query {:ubid "LANDSAT_5/TM/sr_band1"}]
    (spec/find (:database sys) {:ubid "LANDSAT_5/TM/sr_band1"}))

  ;; Getting some tiles
  (let [query {:x -2062080
               :y 2952960
               :acquired ["2002-01-01" "2003-01-01"]
               :ubid "LANDSAT_5/TM/sr_band1"}]
    (tile/find (:database sys) query))

  ;; Getting scene metadata
  (let [query {:source "LT50470282002001LGS01"}]
    (scene/find (:database sys) query)))
