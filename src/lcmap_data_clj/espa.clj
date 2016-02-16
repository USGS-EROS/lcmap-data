(ns lcmap-data-clj.espa
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :refer :all]
            [clojure.tools.logging :as log]))

(defn solar-angles->map
  "Convert a solar_angles element to a map"
  [global]
  (let [sazip (xml1-> global :solar_angles)
        to-double #(if (some? %) (java.lang.Double/parseDouble %))
        props {:zenith  (to-double (attr sazip :zenith))
               :azimuth (to-double (attr sazip :azimuth))
               :units   (attr sazip :units)}]
    props))

(defn lpgs-path
  "Retrieve path to LPGS metadata file"
  [global]
  (let [path (xml1-> global :lpgs_metadata_file text)]
    path))

(defn source-scene
  "Extract implicit scene ID from relative LPGS metadata file path"
  [lpgs-path]
  (re-find #"[A-Z0-9]+" lpgs-path))

(defn global->map
  "Convert a global_metadata element to a map"
  [root]
  (let [gmzip (xml1-> root :global_metadata)]
    {:satellite    (xml1-> gmzip :satellite text)
     :instrument   (xml1-> gmzip :instrument text)
     :provider     (xml1-> gmzip :data_provider text)
     :acquired     (xml1-> gmzip :acquisition_date text)
     :source       (-> gmzip lpgs-path source-scene)
     :solar-angles (solar-angles->map gmzip)}))

(defn data-range->list
  "Convert a valid_range element into a list"
  [band]
  (if-let [element (xml1-> band :valid_range)]
    [(attr element :min)
     (attr element :max)]))

(defn mask-values->map
  "Convert a class_values element into a map"
  [band]
  (if-let [items (concat (xml-> band :class_values :class)
                         (xml-> band :bitmap_description :bit))]
    (into {} (for [item items]
               [(Integer/parseInt (attr item :num)) (text item)]))))

(defn bands->list
  "Convert all band elements into a list of maps"
  [root]
  (for [band (xml-> root :bands :band)
        :let [props {:file-name  (xml1-> band :file_name text)
                     :band-short-name (xml1-> band :short_name text)
                     :band-long-name  (xml1-> band :long_name text)
                     :band-category   (attr band :category)
                     :band-product    (attr band :product)
                     :band-name       (attr band :name)
                     :data-type  (attr band :data_type)
                     :data-fill  (some-> (attr band :fill_value) Short/parseShort)
                     :data-scale (some-> (attr band :scale_factor) Double/parseDouble)
                     :data-range (map #(if (some? %) (Integer/parseInt %))
                                      (data-range->list band))
                     :data-units (attr band :data_units)
                     :data-mask  (mask-values->map band)}]]
       props))

(defn parse-metadata
  "Create a map from ESPA archive XML metadata"
  [path]
  (log/debug "Parsing metadata file:" path)
  (let [data   (xml/parse path)
        root   (zip/xml-zip data)
        global (global->map root)
        bands  (bands->list root)]
    (map #(merge global %) bands)))

(defn find-metadata
  "Gets the path to the metadata file of an ESPA archive.

  If multiple XML files are present then the first match
  is used -- this shouldn't be the case and is silently
  ignored for now."
  [path]
  (let [files (-> path io/file file-seq)
        names (map #(.getPath ^java.io.File %) files)
        xml   (filter #(re-find #".+\.xml" %) names)]
    (first xml)))

(defn load-metadata
  "Find and parse metadata at path (a directory)"
  [path]
  (let [xml-path (find-metadata path)
        xml-data (parse-metadata xml-path)]
    xml-data))
