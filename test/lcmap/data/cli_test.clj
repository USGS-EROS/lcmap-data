(ns lcmap.data.cli-test
  (:require [clojure.test :refer :all]
            [lcmap.data.shared-test :as shared]
            [lcmap.data.cli :as cli]
            [clojurewerkz.cassaforte.client :as client]))

(deftest exec-cql-test
  (let [sys shared/test-system
        missing {:arguments ["--file" "test/schema.cql.missing"]}
        present {:arguments ["--file" "test/schema.cql"]}]
    ;; Use redefs to prevent system from exiting during exception
    ;; handling; the CLI ns has a very broad exception handler that
    ;; exits if an error is encountered (by design). Also, prevent
    ;; CQL from being executed.
    (with-redefs [cli/exit (constantly :exit)
                  client/execute (constantly :execute)]
      ;; this counts the number of statements in a contrived file.
      ;; the cassaforte execute is redefined, so nothing actually
      ;; gets run
      (is (= 3 (count (cli/exec-cql "exec-cql" sys present))))
      ;; a missing schema file should raise an unhandled exception
      ;; for now...
      (is (thrown? java.io.FileNotFoundException
                   (cli/exec-cql "exec-cql" sys missing))))))
