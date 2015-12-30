(ns lcmap-data-clj.ingest-test
  (:require [clojure.test :refer :all]
            [lcmap-data-clj.ingest :refer :all]
            [lcmap-data-clj.shared-test :as shared]
            :reload))

;;; These tests use real ESPA data. The two most important functions
;;; during ingest are band-seq and tile-seq. Testing these covers
;;; a number of other supporting functions.

(deftest ^:integration band-seq-test
  (testing "Landsat 8 ESPA output"
    (let [path (shared/L8 :dir)
          bands (band-seq path shared/test-system)]
      (is (= 20 (count bands)))))
  (testing "Landsat 7 ESPA output"
    (let [path (shared/L7 :dir)
          bands (band-seq path shared/test-system)]
      (is (= 25 (count bands)))))
  (testing "Landsat 5 ESPA output"
    (let [path (shared/L5 :dir)
          bands (band-seq path shared/test-system)]
      (is (= 25 (count bands)))))
  (testing "non-existent espa output"
    (let [path "test/data/not-a-dir"
          bands (band-seq path shared/test-system)]
      (is (= 0 (count bands))))))

(deftest ^:integration tile-seq-test
  (let [path (shared/L8 :dir)
        band (first (band-seq path shared/test-system))
        tiles (tile-seq band shared/test-system)]
    (is (= 16 (count tiles)))))

(deftest ^:integration ingest-test
  (testing "Landsat 8 ESPA ingest"
    (let [path (shared/L8 :dir)]
      (ingest path shared/test-system))))

(deftest conforms?-test
  (testing "Aligns to tile spec's implicit raster grid")
  (testing "Does not align to tile spec's implicit raster grid"))

(deftest defined?-test
  (testing "Has data but no spec")
  (testing "Has spec but no data")
  (testing "Has spec and data"))

(deftest get-bounds-test
  (testing "It just works"))

(deftest get-frame-test
  (testing "Bounds are an even multiple of the tile size and offset"))

(deftest band->fill-buffer-test
  (testing "Fill buffer size matches tile size"))

(deftest get-ubid-test
  (testing "Generating a UBID for a band"
    (let [band {:mission "LANDSAT_8"
                :instrument "OLI_TIRS"
                :name "sr_band1"}
          ubid (get-ubid band)]
      (is (= "LANDSAT_8/OLI_TIRS/sr_band1")))))
