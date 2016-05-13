(ns lcmap.data.shared-test
  (:require [clojure.tools.logging :as logging]
            [com.stuartsierra.component :as component]
            [lcmap.data.system :as system]
            [lcmap.data.config :as config]
            [lcmap.data.components.database :as db]
            [lcmap.config.helpers :as cfg-help]
            [leiningen.core.utils :as lein-utils]
            [leiningen.core.project :as lein-proj]
            [dire.core :refer [with-handler!]])
  (:import  [com.datastax.driver.core.exceptions NoHostAvailableException]))

(def cfg-opts (merge config/defaults {:ini "test/lcmap.test.ini"}))
(def cfg-data (-> (cfg-help/init-cfg cfg-opts) :lcmap.data))

(with-handler! #'component/start
  "Build systems that only run unit tests will fail to start the db
  component"
  [NoHostAvailableException]
  (fn [e & args]
    ;; XXX is there a way to reliably get test selectors?
    ;;
    (logging/warn "no db host -- not unusual")
    args))

(def test-system (-> (system/build cfg-opts) (component/start)))

(def L5 "test/data/ESPA/CONUS/ARD/LT50470272010327-SC20151230101810")
(def L7 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
(def L8 "test/data/ESPA/CONUS/ARD/LC80460272015302-SC20151230102540")
