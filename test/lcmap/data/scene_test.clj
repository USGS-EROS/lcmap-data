(ns lcmap.data.scene-test
  (:require [clojure.test :refer :all]
            [lcmap.data.shared-test :as shared]
            [lcmap.data.scene :as scene]))

(deftest save-test
  (testing "save"
    (let [db (:database shared/test-system)
          res (scene/save db {:source "scene-bar" :ubid "band-1"})]
      (is (some? res)))))

(deftest find-test
  (testing "find entire scene"
    (let [db (:database shared/test-system)
          res (scene/find db {:source "scene-foo"})]
      (is (some? res)))))

(deftest column-names-test
  (testing "column names for scene"
    (let [db (:database shared/test-system)
          res (scene/column-names db)]
      (is (= [:source :ubid])))))
