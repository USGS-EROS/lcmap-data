(ns lcmap.data.espa-test
  (:require [clojure.test :refer :all]
            [lcmap.data.shared-test :as shared]
            [lcmap.data.espa :as espa]))

(def xml-paths {:L8 "test/data/ESPA/CONUS/metadata/LC80460272015302LGN01.xml"
                :L7 "test/data/ESPA/CONUS/metadata/LE70460272010328EDC00.xml"
                :L5 "test/data/ESPA/CONUS/metadata/LT50470272010327EDC00.xml"})

(deftest load-test
  (testing "parsing global and band metadata together"
    (let [bands (espa/load (:L8 xml-paths))]
      (is (every? :global_metadata bands))
      (is (every? :ubid bands))
      (is (every? :path bands))
      (is (every? :acquired bands)))))

(deftest load-global-metadata-test
  (testing "parsing global metadata"
    (let [global (espa/load-global-metadata (:L8 xml-paths))
          actual (set (keys global))
          expected #{:provider :satellite :instrument
                     :acquired :solar_angles :source
                     :lpgs_file}]
      (is (empty? (clojure.set/difference actual expected))))))

(deftest load-bands-test
  (let [bands (espa/load (:L8 xml-paths))
        masks (filter (comp seq :data_mask) bands)]
    (testing "number of bands"
      (is (= (count bands) 20)))
    (testing "band specific attributes"
      (let [band (first bands)]
        (is (= "toa_band1" (:band_name band)))
        (is (= "LC8TOA" (:band_short_name band)))
        (is (= "band 1 top-of-atmosphere reflectance" (:band_long_name band)))
        (is (= "image" (:band_category band)))
        (is (= "toa_refl" (:band_product band)))
        (is (= "INT16" (:data_type band)))
        (is (= -9999 (:data_fill band)))
        (is (= 0.0001 (:data_scale band)))
        (is (= [-2000 16000] (:data_range band)))
        (is (empty? (:data_class band)))))
    (testing "mask bands"
      (is (= (count masks) 3)))
    (testing "cloud mask"
      (let [mask (-> masks first :data_mask)]
        (is (= mask {0 "cirrus cloud"
                     1 "cloud"
                     2 "adjacent to cloud"
                     3 "cloud shadow"
                     4 "aerosol"
                     5 "aerosol"
                     6 "unused"
                     7 "internal test"}))))))
