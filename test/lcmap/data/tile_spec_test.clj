(ns lcmap.data.tile-spec-test
  (:require [clojure.test :refer :all]
            [lcmap.data.shared-test :as shared]
            [lcmap.data.fixtures :as fixtures]
            [lcmap.data.tile-spec :as tile-spec]))

(deftest ^:integration database-interaction
  (shared/with-system [system shared/cfg-opts]
    (testing "getting column names for the tile-spec table"
      (let [names (set (tile-spec/column-names (:database system)))]
        (is (contains? names :pixel_x))))
    (testing "find individual tile specs"
      (let [specs (tile-spec/find (:database system)                                  {:ubid "FOO"})]))
    (testing "saving invalid tile data"
      (let [specs (tile-spec/save (:database system)
                                  {:ubid "TBD" :keyspace_name "test.keyspace" :table_name "test.table"})])
    (testing "transforming a band into a tile-spec"))))
