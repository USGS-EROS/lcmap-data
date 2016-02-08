;;;; LCMAP Data core namespace
;;;;
;;;; This namespace defines the command line behaviors provided by this
;;;; library. It provides two functions invoked via Leiningen.
;;;;
;;;; 1. CQL Exection: used to load the schema
;;;; 2. Save metadata as tile specification.
;;;; 2. Save raster data as tiles.
(ns lcmap-data-clj.core
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql    :as cql]
            [clojure.pprint                 :as pprint]
            [clojure.tools.cli              :as cli]
            [clojure.tools.logging          :as log]
            [clojure.java.io                :as io]
            [com.stuartsierra.component     :as component]
            [lcmap-data-clj.system          :as sys]
            [lcmap-data-clj.ingest          :refer [ingest adopt]]
            [lcmap-data-clj.util            :as util]))

(def cli-option-specs [["-h" "--hosts HOST1,HOST2,HOST3"
                        :parse-fn #(clojure.string/split % #"[, ]")
                        :default (System/getenv "LCMAP_HOSTS")]
                       ["-u" "--username USERNAME"
                        :default (System/getenv "LCMAP_USER")]
                       ["-p" "--password PASSWORD"
                        :default (System/getenv "LCMAP_PASS")]
                       ["-k" "--spec-keyspace SPEC_KEYSPACE"
                        :default (System/getenv "LCMAP_SPEC_KEYSPACE")]
                       ["-t" "--spec-table SPEC_TABLE"
                        :default (System/getenv "LCMAP_SPEC_TABLE")]
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

(defn cli-info
  [system config-map]
  (pprint/pprint config-map))

(defn cli-main
  "Entry point for command line execution"
  [& args]
  (let [cli-args (cli/parse-opts args cli-option-specs)
        db-opts  {:db {:hosts (get-in cli-args [:options :hosts])
                       :credentials (select-keys cli-args [:username :password])}}
        env-opts (util/get-config)
        combined (util/deep-merge env-opts db-opts)
        system   (component/start (sys/build combined))
        cmd      (-> cli-args :arguments first)]
    (try
      (cond (= cmd "exec") (cli-exec-cql system cli-args)
            (= cmd "tile") (cli-make-tiles system cli-args)
            (= cmd "spec") (cli-make-specs system cli-args)
            (= cmd "info") (cli-info system combined)
            :else (println "I have no idea what to do with" cmd))
      (component/stop system)
      (System/exit 0)
      (catch Exception ex
        (log/error ex)
        (System/exit 1)))))

