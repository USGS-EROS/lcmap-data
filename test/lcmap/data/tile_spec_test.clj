(ns lcmap.data.tile-spec-test
  (:require [clojure.test :refer :all]
            [lcmap.data.shared-test :as shared]
            [lcmap.data.fixtures :as fixtures]
            [lcmap.data.tile-spec :as tile-spec]))

(use-fixtures :each fixtures/tile-spec-fixtures)

;;

(deftest ^:integration find-test
  (testing "find individual tile specs"
    (let [db (:database shared/test-system)
          s1 (tile-spec/find db {:ubid "test/1"})
          s2 (tile-spec/find db {:ubid "test/2"})
          s3 (tile-spec/find db {:ubid "test/3"})
          s4 (tile-spec/find db {:ubid "fake"})]
      (is (seq s1))
      (is (seq s2))
      (is (seq s3))
      (is (empty? s4))))
  (testing "find all tile specs for a keyspace-table combo"
    (let [db (:database shared/test-system)
          ts (tile-spec/find db {:keyspace_name "lcmap_test"
                                 :table_name "conus"})]
      (is (= 3 (count ts))))))


(deftest ^:integration save-test
  (testing "saving tile data"
    ;; XXX Saving a tile-spec does not enforce any sort of
    ;; validation. This is more of an AS-IS test, that can
    ;; be redefined as save becomes more robust.
    (let [db (:database shared/test-system)
          result (tile-spec/save db {:ubid "test"
                                     :keyspace_name "test"
                                     :table_name "test"})]
      (is (empty? result)))))
