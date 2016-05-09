(ns lcmap.data.config
  (:require [lcmap.config.helpers :as cfg]
            [schema.core :as schema]))

(def opt-spec [[nil "--foo VALUE"]
               [nil "--bar VALUE"]
               [nil "--baz VALUE"]])

;;; configuration schemas

(def db-schema
  {:db-hosts       [schema/Str]
   :db-user        schema/Str
   :db-pass        schema/Str
   :spec-keyspace  schema/Str
   :spec-table     schema/Str
   :scene-table    schema/Str
   :scene-keyspace schema/Str})

;;; Composition of all sub-schemas. Notice the convention:
;;; the keywords at the root of the map match the component's
;;; namespace so that each component knows where to locate
;;; its specific configuration map.

(def cfg-schema
  {:lcmap.data db-schema
   schema/Keyword schema/Any})

(def defaults
  {:ini (clojure.java.io/file (System/getenv "HOME") ".usgs" "lcmap.ini")
   :spec opt-spec
   :args *command-line-args*
   :schema cfg-schema})

(cfg/init-cfg defaults)
