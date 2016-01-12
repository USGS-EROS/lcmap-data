;;;; LCMAP Data core namespace
;;;;
;;;; This namespace defines the command line behaviors provided by this
;;;; library. It provides two functions invoked via Leiningen.
;;;;
;;;; 1. CQL Exection: used to load the schema and create initial tile specs.
;;;; 2. Ingest ESPA archives.
(ns lcmap-data-clj.core
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql    :as cql]
            [clojure.tools.cli              :as cli]
            [clojure.tools.logging          :as log]
            [clojure.java.io                :as io]
            [com.stuartsierra.component     :as component]
            [lcmap-data-clj.system          :as sys]
            [lcmap-data-clj.ingest          :refer [ingest adopt]]
            [lcmap-data-clj.util            :as util]))


(def db-option-specs [["-h" "--hosts HOST1,HOST2,HOST3"
                       :default (or (System/getenv "LCMAP_CASSANDRA_HOSTS"))
                       :parse-fn #(clojure.string/split % #"[, ]")]
                      ["-k" "--spec-keyspace SPEC_KEYSPACE"
                       :default "lcmap"]
                      ["-t" "--spec-table SPEC_TABLE"
                       :default "tile_specs"]
                      ["-c" "--cql PATH_TO_CQL"
                       :default "resources/schema.cql"]])

(defn execute-cql
  "Execute all statements in file specified by path"
  [system path]
  (let [conn (-> system :database :session)
        cql-file (slurp path)
        statements (map clojure.string/trim (clojure.string/split cql-file #";"))]
    (doseq [stmt (remove empty? statements)]
      (try
        (cc/execute conn stmt)
        (catch Exception ex
          (log/error "error executing CQL" (ex-data ex)))))))

(defn cli-exec-cql
  "Executes CQL (useful for creating schema and seeding data)"
  [system opts]
  (let [path (-> opts :options :cql)]
    (execute-cql system path)))

(defn cli-make-tiles
  "Generate tiles from an ESPA archive"
  [system opts]
  (let [paths (-> opts :arguments rest)]
    (doseq [path paths]
      (util/with-temp [dir path]
        (ingest dir system)))))

(defn cli-make-specs
  "Generate specs from an ESPA archive"
  [system opts]
  (let [paths (-> opts :arguments rest)]
    (doseq [path paths]
      (util/with-temp [dir path]
        (adopt dir system)))))

(defn cli-main
  "Entry point for command line execution"
  [& args]
  (let [db-opts (cli/parse-opts args db-option-specs)
        system  (component/start (sys/build {:db (:options db-opts)}))
        cmd     (-> db-opts :arguments first)]
    (try
      (cond (= cmd "exec") (cli-exec-cql system db-opts)
            (= cmd "tile") (cli-make-tiles system db-opts)
            (= cmd "spec") (cli-make-specs system db-opts)
            :else (println "I have no idea what to do with" cmd))
      (component/stop system)
      (System/exit 0)
      (catch Exception ex
        (log/error ex)
        (System/exit 1)))))

