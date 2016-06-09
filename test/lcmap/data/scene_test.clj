(ns lcmap.data.scene-test
  (:require [clojure.test :refer :all]
            [lcmap.data.shared-test :as shared]
            [lcmap.data.scene :as scene]))

(deftest ^:integration database-tests
  (shared/with-system [system shared/cfg-opts]
    (testing "find entire scene"
      (let [res (scene/find (:database system) {:source "scene-foo"})]
        (is (some? res))))
    (testing "save"
      (let [res (scene/save (:database system) {:source "scene-bar" :ubid "band-1"})]
        (is (some? res))))
    (testing "column names for scene"
      (let [res (scene/column-names (:database system))]
        (is (= [:source :ubid]))))))
