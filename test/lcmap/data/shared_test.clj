(ns lcmap.data.shared-test
  (:require [clojure.tools.logging :as logging]
            [com.stuartsierra.component :as component]
            [dire.core :refer [with-handler!]]
            [lcmap.data.system :as system]
            [lcmap.data.config :as config]
            [lcmap.config.helpers :as config-helpers])
  (:import  [com.datastax.driver.core.exceptions NoHostAvailableException]))

(def cfg-file (clojure.java.io/file config-helpers/*lcmap-config-dir* "lcmap.test.ini"))

(def cfg-opts (merge config/defaults {:ini cfg-file}))

(defmacro with-system
  [[binding cfg-opts] & body]
  `(let [~binding (component/start (system/build ~cfg-opts))]
     (try
       (do ~@body)
       (finally
         (component/stop ~binding)))))
