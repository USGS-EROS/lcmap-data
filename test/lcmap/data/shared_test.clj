(ns lcmap.data.shared-test
  (:require [clojure.tools.logging :as logging]
            [com.stuartsierra.component :as component]
            [dire.core :refer [with-handler!]]
            [lcmap.data.system :as system]
            [lcmap.data.config :as config]
            [lcmap.config.helpers :as config-helpers]))

(with-handler! #'component/start
  [NoHostAvailableException]
  (fn [e & args]
    (logging/warn "no db host -- not unusual")
    args))

(def cfg-file (clojure.java.io/file config-helpers/*lcmap-config-dir* "lcmap.test.ini"))

(def cfg-opts (merge config/defaults {:ini cfg-file}))

(def test-system (-> (system/build cfg-opts)
                     (component/start)))

(def L5 "test/data/ESPA/CONUS/ARD/LT50470272010327-SC20151230101810")
(def L7 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
(def L8 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
