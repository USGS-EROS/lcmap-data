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
            [lcmap.config.helpers :as config-helpers]
            [lcmap.data.system :as sys]
            [lcmap.data.ingest :as ingest]
            [lcmap.data.adopt :as adopt]
            [lcmap.data.util :as util]
            [lcmap.data.config :as config])
  (:gen-class))

;;; command: lein lcmap run-cql

(defn execute-cql
  "Execute all statements in file specified by path"
  [system path]
  (let [conn (-> system :database :session)
        cql-file (slurp path)
        statements (map clojure.string/trim (clojure.string/split cql-file #";"))]
    (for [stmt (remove empty? statements)]
      (cc/execute conn stmt))))

(def exec-cql-spec
  [["-f" "--file PATH"]])

(defn exec-cql
  "Executes CQL (useful for creating schema and seeding data)"
  [cmd system cli-args]
  (log/infof "Running command: '%s'" cmd)
  (let [opts (cli/parse-opts (:arguments cli-args) exec-cql-spec)
        path (get-in opts [:options :file])]
    (doall (execute-cql system path))))

;;; command: lein lcmap make-tiles

(def make-tile-opts
  [[nil "--checksum"
    "Produce file containing tile checksums"]])

(defn make-tiles
  "Generate tiles from an ESPA archive"
  [cmd system cli-args]
  (log/infof "Running command: '%s'" cmd)
  (let [opts  (cli/parse-opts (:arguments cli-args) make-tile-opts)
        paths (-> opts :arguments rest)
        db    (:database system)]
    (doseq [path paths]
      (log/infof "archive-start: %s" path)
      (try
        (util/with-temp [dir path]
          (ingest/process-scene db dir))
        (catch RuntimeException e
          (log/errorf "archive-fail: %s %s" path (.getMessage e)))
        (catch java.io.IOException e
          (log/errorf "archive-fail: %s %s" path (.getMessage e))))
      (log/infof "archive-done: %s" "archive-done" path))))

;;; command: lein lcmap make-specs

(defn parse-shape
  "Helper function to convert data-shape param into list of numbers"
  [shape]
  (->> shape
       (#(clojure.string/split % #":"))
       (map #(Integer/parseInt %))))

(def make-specs-opts
  [[nil "--tile-keyspace TARGET_KEYSPACE"
    "Keyspace name containing tile table."]
   [nil "--tile-table TARGET_TILE_TABLE"
    "Table name to store tiles matching derived spec."]
   [nil "--tile-size X:Y"
    (str "Colon-separated pixel values for the width:height shape of tiles "
         "to create during ingest.")
    :default [128 128]
    :parse-fn parse-shape]])

(defn make-specs
  "Generate specs from an ESPA archive"
  [cmd system cli-args]
  (log/infof "Running command: '%s'" cmd)
  (let [opts  (cli/parse-opts (:arguments cli-args) make-specs-opts)
        paths (-> opts :arguments rest)
        args  (:options opts)
        db    (:database system)]
    (doseq [path paths]
      (log/infof "archive-start: %s" path)
      (util/with-temp [dir path]
        (adopt/process-scene db dir args))
      (log/infof "archive-done: %s" path))))

;;; command: lein lcmap --info

(defn show-info
  "Display combined cli options, environment, and profile values"
  [config-map]
  (log/info "Running command: 'info'")
  (println "lcmap.data information:\n")
  (pprint/pprint config-map))

;;; command: lein lcmap --help

(defn help
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
        ""]
       (string/join \newline)))

(defn exit
  ([status]
    (System/exit status))
  ([status msg]
    (println msg)
    (exit status)))

;;; entry point related functions

(defn run
  "Init system and invoke function for user specified command"
  [cli-args]
  (let [cmd (-> cli-args :arguments first)
        cfg-opts (merge config/defaults {:args (cli-args :arguments)})
        system (component/start (sys/build cfg-opts))]
    (cond (= cmd "run-cql") (exec-cql cmd system cli-args)
          (= cmd "make-specs") (make-specs cmd system cli-args)
          (= cmd "make-tiles") (make-tiles cmd system cli-args)
          :else (log/error "Invalid command:" cmd))
    (component/stop system)
    (exit 0)))

(def main-opts
  [["-h" "--help"]
   ["-i" "--info"]])

(defn -main
  "Entry point for command line execution"
  [& args]
  ;; Use :in-order true because sub-commands may expect
  ;; to parse options of their own.
  (let [cli-args (cli/parse-opts args main-opts :in-order true)
        cfg (config-helpers/init-cfg (merge config/defaults {:args args}))
        help? (:help (:options cli-args))
        info? (:info (:options cli-args))]
    (cond
      help? (exit 0 (help (:summary cli-args)))
      info? (show-info cfg)
      :else (run cli-args))))

;;; exception handlers

(with-handler! #'-main
  java.lang.Exception
  (fn [e & args]
    (log/error e)
    (exit 1)))
