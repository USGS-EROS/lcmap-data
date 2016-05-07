(ns lcmap.data.shared-test
  (:require [com.stuartsierra.component :as component]
            [lcmap.data.system :as system]
            [lcmap.data.config :as config]))

(def cfg-opts (merge config/defaults {:ini "test/lcmap.test.ini" :args []}))
(def test-system (component/start (system/build cfg-opts)))

(-> test-system :cfg :lcmap.data.components.db)

(def L5 "test/data/ESPA/CONUS/ARD/LT50470272010327-SC20151230101810")
(def L7 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
(def L8 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
