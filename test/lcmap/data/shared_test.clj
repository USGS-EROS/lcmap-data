;;;; LCMAP Data shared test namespace provides fixture data of sorts to
;;;; for other tests.
(ns lcmap.data.shared-test
  (:require [leiningen.core.project :as lein-prj]
            [com.stuartsierra.component :as component]
            [lcmap.data.ingest :as ingest]
            [lcmap.data.system :as system]))

(def test-system (component/start (system/build (lein-prj/read))))

(def L8 {:dir "test/test-data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540"
         :xml "LC80460272015302LGN01.xml"
         :img "LC80460272015302LGN01_toa_band1.tif"})

(def L7 {:dir "test/test-data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540"
         :xml "LC80460272015302LGN01.xml"
         :img "LC80460272015302LGN01_toa_band1.tif"})

(def L5 {:dir "test/test-data/ESPA/CONUS/ARD/LT50470272010327-SC20151230101810"
         :xml "LT50470272010327-SC20151230101810/LT50470272010327EDC00.xml"
         :img "LT50470272010327-SC20151230101810/LT50470272010327EDC00_sr_band1.tif"})
