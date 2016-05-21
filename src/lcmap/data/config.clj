(ns lcmap.data.config
  "Provides schemas for validating configuration data and values."
  (:require [lcmap.config.helpers :as helpers]
            [schema.core :as schema]))

;;; configuration schemas

(def data-schema
  {:lcmap.data {:db-hosts       [schema/Str]
                :db-user        schema/Str
                :db-pass        schema/Str
                :spec-keyspace  schema/Str
                :spec-table     schema/Str
                :scene-table    schema/Str
                :scene-keyspace schema/Str
                schema/Keyword  schema/Str}})

(def logging-schema
  {:lcmap.logging {:level schema/Str
                   :namespaces [schema/Str]}})

(def cfg-schema
  (merge data-schema
         logging-schema
         {schema/Keyword schema/Any}))

;;; cli opt-specs placeholder

(def opt-spec [])

;;; Default parameters for use with lcmap.config.helpers/init-cfg

(def defaults
  {:ini helpers/*lcmap-config-ini*
   :args *command-line-args*
   :spec opt-spec
   :schema cfg-schema})
