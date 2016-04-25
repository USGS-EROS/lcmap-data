(ns lcmap.data.cli
  "Provide command-line interface to tile and tile-spec features."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]
            [com.stuartsierra.component :as component]
            [dire.core :refer [with-handler!]]
            [twig.core :as twig]
            [lcmap.data.system :as sys]
            [lcmap.data.ingest :as ingest]
            [lcmap.data.adopt :as adopt]
            [lcmap.data.util :as util])
  (:gen-class))

(defn parse-int
  "Helper function"
  [x]
  (Integer/parseInt x))

(defn parse-shape
  "Helper function to convert data-shape param into list of numbers"
  [shape]
  (->> shape
       (#(clojure.string/split % #":"))
       (map parse-int)))

(def option-specs
  [["-h" "--help"]
   ["-H" "--hosts HOST1,HOST2,HOST3" "List of hosts"
    :parse-fn #(clojure.string/split % #"[, ]")
    :default (clojure.string/split
      (or (System/getenv "LCMAP_HOSTS") "") #"[, ]")]
   ["-u" "--username USERNAME" "Cassandra user ID"
    :default (System/getenv "LCMAP_USER")]
   ["-p" "--password PASSWORD" "Cassandra password"
    :default (System/getenv "LCMAP_PASS")]
   ["-k" "--spec-keyspace SPEC_KEYSPACE" ""
    :default (System/getenv "LCMAP_SPEC_KEYSPACE")]
   ["-t" "--spec-table SPEC_TABLE" ""
    :default (System/getenv "LCMAP_SPEC_TABLE")]
   [nil "--tile-table TILE_TABLE"
    :default (System/getenv "LCMAP_TILE_TABLE")]
   [nil "--tile-keyspace TILE_KEYSPACE"
    :default (System/getenv "LCMAP_TILE_KEYSPACE")]
   [nil "--tile-size x:y"
    (str "Colon-separated pixel values for the width:height shape of tiles "
         "to create during ingest.")
    :default [256 256]
    :parse-fn parse-shape]
   ;; XXX load default from proper env variable? parse string?
   ["-b" "--batch-size n"
    "The number of tiles to process at a time. Placeholder, not used."
    :default (or (System/getenv "LCMAP_BATCH_SIZE") 50)
    :parse-fn parse-int]
   [nil "--file PATH_TO_CQL" ""
    :default "resources/schema.cql"]
   ])

(defn execute-cql
  "Execute all statements in file specified by path"
  [system path]
  (let [conn (-> system :database :session)
        cql-file (slurp path)
        statements (map clojure.string/trim (clojure.string/split cql-file #";"))]
    (doseq [stmt (remove empty? statements)]
      (cc/execute conn stmt))))

(defn exec-cql
  "Executes CQL (useful for creating schema and seeding data)"
  [cmd system opts]
  (log/infof "Running command: '%s'" cmd)
  (let [path (-> opts :options :file)]
    (execute-cql system path)))

(defn make-tiles
  "Generate tiles from an ESPA archive"
  [cmd system opts]
  (log/infof "Running command: '%s'" cmd)
  (let [paths (-> opts :arguments rest)
        db    (system :database)]
    (doseq [path paths]
      (util/with-temp [dir path]
        (ingest/process-scene db dir)))))

(defn make-specs
  "Generate specs from an ESPA archive"
  [cmd system opts]
  (log/infof "Running command: '%s'" cmd)
  (let [paths (-> opts :arguments rest)
        args  (:options opts)
        db    (:database system)]
    (doseq [path paths]
      (util/with-temp [dir path]
        (adopt/process-scene db dir args)))))

(defn show-info
  "Display combined cli options, environment, and profile values"
  [config-map]
  (log/info "Running command: 'info'")
  (println "lcmap.data information:\n")
  (pprint/pprint config-map))


(defn usage
  "Produce help text"
  [options-summary]
  (->> ["The command line interface for lcmap.data."
        ""
        "Usage: lein lcmap [options] command"
        ""
        "Options:"
        options-summary
        ""
        "Available commands:"
        "  make-specs   Extract an ESPA archive tile specification, saving to the database"
        "  make-tiles   Ingest tile data "
        "  run-cql      Run a Cassandra query stored in CQL file"
        "  show-config  Display basic tool info such as configuration data"]
       (string/join \newline)))

(defn exit
  ([status]
    (System/exit status))
  ([status msg]
    (println msg)
    (exit status)))

(defn run
  "Init system and invoke function for user specified command"
  [cli-args combined-cfg]
  (twig/set-level! ['lcmap.data] :info)
  (let [cmd (-> cli-args :arguments first)
        system (component/start (sys/build combined-cfg))]
    (cond (= cmd "run-cql") (exec-cql cmd system cli-args)
          (= cmd "make-specs") (make-specs cmd system cli-args)
          (= cmd "make-tiles") (make-tiles cmd system cli-args)
          :else (log/error "Invalid command:" cmd))
    (component/stop system)
    (exit 0)))

(defn -main
  "Entry point for command line execution"
  [& args]
  (let [cli-args (cli/parse-opts args option-specs)
        db-opts  {:db {:hosts (get-in cli-args [:options :hosts])
                       :credentials (select-keys (cli-args :options) [:username :password])}}
        env-opts (util/get-config)
        combined-cfg (util/deep-merge env-opts db-opts {:opts (:options cli-args)})
        help? (:help (:options cli-args))
        info? (= "show-info" (first (:arguments cli-args)))]
    (cond
      help? (exit 0 (usage (:summary cli-args)))
      info? (show-info combined-cfg)
      :else (run cli-args combined-cfg))))

(with-handler! #'-main
  java.lang.Exception
  (fn [e & args]
    (log/error e)
    (System/exit 1)))
