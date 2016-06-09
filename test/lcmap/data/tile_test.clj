(ns lcmap.data.tile-test
  (:require [clojure.test :refer :all]
            [lcmap.data.shared-test :as shared]
            [lcmap.data.tile :as tile]
            [lcmap.data.tile-spec :as tile-spec]))

(deftest ^:unit snap-test
  (testing "without shift"
    (let [spec {:tile_x (*  30 32) ;  30m * 32px west-to-east
                :tile_y (* -30 32) ; -30m * 32px north-to-south
                :shift_x 0
                :shift_y 0}
          [x y] (tile/snap 0 0 spec)]
      (is (= 0 x))
      (is (= 0 y))))
  (testing "with shift"
  ;; Shift is in terms of units from west-to-east and north-to-south
  ;; for the x- and y-axis respectively.
    (let [spec {:tile_x (*  30 32)
                :tile_y (* -30 32)
                :shift_x  15
                :shift_y -15}
          [x y] (tile/snap 0 0 spec)]
      (is (= -15 x))
      (is (=  15 y)))))

;; XXX requires sensible fixtures
(deftest ^:integration find-test
  (testing "finding tiles"))

;; XXX tbd
(deftest ^:integration save-test
  (testing "saving tiles"))
