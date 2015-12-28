;;;; LCMAP Data shared test namespace provides fixture data of sorts to
;;;; for other tests.
(ns lcmap-data-clj.shared-test
  (:require [lcmap-data-clj.system :as system]
            [lcmap-data-clj.ingest :as ingest]
            [leiningen.core.project :as lein-prj]
            [com.stuartsierra.component :as component]))

(def test-system (component/start (system/build (lein-prj/read))))

(def scene {:dir "test/data/espa/L8"
            :xml "LC80460272013104LGN01.xml"
            :img "LC80460272013104LGN01_toa_band1.tif"})
