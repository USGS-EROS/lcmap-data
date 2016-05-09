(ns lcmap.data.config
  "Provides schemas for validating "
  (:require [lcmap.config.helpers :refer :all]
            [schema.core :as schema]))

;;; configuration schemas

(def db-schema
  {:db-hosts       [schema/Str]
   :db-user        schema/Str
   :db-pass        schema/Str
   :spec-keyspace  schema/Str
   :spec-table     schema/Str
   :scene-table    schema/Str
   :scene-keyspace schema/Str})

(def cfg-schema
  {:lcmap.data db-schema
   schema/Keyword schema/Any})

;;; cli opt-specs placeholder

(def opt-spec [])

;;; Default parameters for use with lcmap.config.helpers/init-cfg

(def defaults
  {:ini *lcmap-config-ini*
   :args *command-line-args*
   :spec opt-spec
   :schema cfg-schema})
