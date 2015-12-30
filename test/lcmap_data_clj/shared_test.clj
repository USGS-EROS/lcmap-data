;;;; LCMAP Data shared test namespace provides fixture data of sorts to
;;;; for other tests.
(ns lcmap-data-clj.shared-test
  (:require [lcmap-data-clj.system :as system]
            [lcmap-data-clj.ingest :as ingest]
            [leiningen.core.project :as lein-prj]
            [com.stuartsierra.component :as component]))

(def test-system (component/start (system/build (lein-prj/read))))

(def L8 {:dir "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540"
         :xml "LC80460272015302LGN01.xml"
         :img "LC80460272015302LGN01_toa_band1.tif"})

(def L7 {:dir "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540"
         :xml "LC80460272015302LGN01.xml"
         :img "LC80460272015302LGN01_toa_band1.tif"})

(def L5 {:dir "test/data/ESPA/CONUS/ARD/LT50470272010327-SC20151230101810"
         :xml "LT50470272010327-SC20151230101810/LT50470272010327EDC00.xml"
         :img "LT50470272010327-SC20151230101810/LT50470272010327EDC00_sr_band1.tif"})
