(ns lcmap-data-clj.espa-test
  (:require [clojure.test :refer :all]
            [lcmap-data-clj.shared-test :as shared]
            [lcmap-data-clj.espa :refer :all]))

(deftest load-metadata-test
  (let [path "test/data/espa/L8"
        bands (load-metadata path)
        masks (filter (comp seq :data-mask) bands)]
    (testing "number of bands"
      (is (= (count bands) 22)))
    (testing "global (common) attributes"
      (is (every? #(= % "LANDSAT_8") (map :satellite bands)))
      (is (every? #(= % "OLI_TIRS") (map :instrument bands)))
      (is (every? #(= % "USGS/EROS") (map :provider bands)))
      (is (every? #(= % "2013-04-14") (map :acquired bands))))
    (testing "band specific attributes"
      (let [band (first bands)]
        (is (= "LC80460272013104LGN01_toa_band1.tif" (:file-name band)))
        (is (= "toa_band1" (:band-name band)))
        (is (= "LC8TOA" (:band-short-name band)))
        (is (= "band 1 top-of-atmosphere reflectance" (:band-long-name band)))
        (is (= "image" (:band-category band)))
        (is (= "toa_refl" (:band-product band)))
        (is (= "INT16" (:data-type band)))
        (is (= -9999 (:data-fill band)))
        (is (= 0.0001 (:data-scale band)))
        (is (= [-2000 16000] (:data-range band)))
        ;; only masks have data classifications
        (is (empty? (:data-class band)))))
    (testing "mask bands"
      (is (= (count masks) 3)))
    (testing "cloud mask"
      (let [mask (-> masks first :data-mask)]
        (is (= mask {0 "cirrus cloud"
                     1 "cloud"
                     2 "adjacent to cloud"
                     3 "cloud shadow"
                     4 "aerosol"
                     5 "aerosol"
                     6 "unused"
                     7 "internal test"}))))))
