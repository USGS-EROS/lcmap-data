;;;; LCMAP Data shared test namespace provides fixture data of sorts to
;;;; for other tests.
(ns lcmap.data.shared-test
  (:require [leiningen.core.project :as lein-prj]
            [com.stuartsierra.component :as component]
            [lcmap.data.ingest :as ingest]
            [lcmap.data.system :as system]
            [lcmap.data.util :as util]))

(def test-system (component/start (system/build (util/get-config))))

(def L5 "test/data/ESPA/CONUS/ARD/LT50470272010327-SC20151230101810")
(def L7 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
(def L8 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
