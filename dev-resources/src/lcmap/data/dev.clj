(ns lcmap.data.dev
  ""
  (:require [lcmap.data.config :as config]
            [lcmap.data.content :as content]
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

  ;; Making some files from tiles
  (let [spec  (first (spec/find (:database sys) {:ubid "LANDSAT_5/TM/sr_band1"}))
        query {:x -2062080 :y 2952960 :acquired ["2002-05-14" "2002-05-21"] :ubid "LANDSAT_5/TM/sr_band1"}
        tiles (tile/find (:database sys) query)]
    (content/create "netCDF" spec tiles))

  ;; Getting scene metadata
  (let [query {:source "LT50470282002001LGS01"}]
    (scene/find (:database sys) query)))

(def ts (tile/find (:database sys) {:x -2062080 :y 2952960 :acquired ["2002-05-14" "2002-05-21"] :ubid "LANDSAT_5/TM/sr_band1"}))
(def shorty (short-array (* 128 128)))
(-> ts first :data (.asShortBuffer) (.get shorty))
(-> ts first :acquired)
(map #(aget shorty %) [1000 2000 3000])
