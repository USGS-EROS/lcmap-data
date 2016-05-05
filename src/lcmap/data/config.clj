(ns lcmap.data.config
  (:require [lcmap.config.helpers :as cfg]
            [schema.core :as schema]))

(def opt-spec [[nil "--foo VALUE"]
               [nil "--bar VALUE"]
               [nil "--baz VALUE"]])

;;; configuration schemas

(def db-cfg-schema
  {:hosts          [schema/Str]
   :user           schema/Str
   :pass           schema/Str
   :spec-keyspace  schema/Str
   :spec-table     schema/Str
   :scene-table    schema/Str
   :scene-keyspace schema/Str
   schema/Keyword  schema/Str})

(def gdal-cfg-schema
  {schema/Keyword schema/Str})

(def logging-cfg-schema
  {schema/Keyword schema/Str})

;;; Composition of all sub-schemas. Notice the convention:
;;; the keywords at the root of the map match the component's
;;; namespace so that each component knows where to locate
;;; its specific configuration map.

(def cfg-schema
  {:lcmap.data.components.db db-cfg-schema
   :lcmap.data.components.gdal gdal-cfg-schema
   ;; permits configs maps for other components
   schema/Keyword schema/Any})

(defn init
  "Produce a validated configuration map. When configuration is
  built in the context of another system, you may want to compose
  a schema for only the components you will use."
  [{:keys [path spec args schema]
    :or   {path (clojure.java.io/file (System/getenv "HOME") ".usgs" "lcmap.ini")
           spec opt-spec
           args *command-line-args*
           schema cfg-schema}}]
  (cfg/init-cfg {:ini  path
                 :args args
                 :spec spec
                 :schema schema}))
