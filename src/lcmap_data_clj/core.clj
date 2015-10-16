(ns lcmap-data-clj.core
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql    :as cql]
            [clojure.tools.cli              :as cli]
            [clojure.java.io                :as io]
            [taoensso.timbre                :as timbre]))

(timbre/refer-timbre)
(timbre/set-level! :debug)

(defn connect
  "Establish a connection to database"
  [& {:keys [hosts]
      :or   {hosts (or (System/getenv "CASSANDRA_HOSTS") "192.168.33.20")}
      :as   args}]
  (let [host-list (clojure.string/split hosts #"[, ]")]
    (debug (str "connecting: " hosts))
    (cc/connect host-list)))

(defn create-schema
  "Execute all statements in file specified by path"
  [conn & {:keys [path]
           :or   {path "resources/schema.cql"}}]
  (let [schema-cql (slurp path)        
        statements (map clojure.string/trim (clojure.string/split schema-cql #";"))]
    (doseq [stmt (remove empty? statements)]
      (try
        (trace "executing: " stmt)
        (cc/execute conn stmt)
        (catch Exception e
          (error (.getMessage e)))))))

(defn run-create-schema [& args]
  (debug "preparing schema initialization")
  (create-schema (connect))
  (debug "finished schema initialization")
  (System/exit 0))
