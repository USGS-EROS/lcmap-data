(defproject  gov.usgs.eros/lcmap-data "0.5.0-SNAPSHOT"
  :description "LCMAP data layer management tools"
  :url "http://github.com/USGS-EROS/lcmap-data"
  :license {:name "NASA Open Source Agreement, Version 1.3"
            :url "http://ti.arc.nasa.gov/opensource/nosa/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.3"]
                 ;; environment/config
                 [gov.usgs.eros/lcmap-config "0.5.0-SNAPSHOT"]
                 ;; logging
                 [twig "0.1.6"]
                 [janino "2.5.10"]
                 ;; checksum (for debug logging)
                 [pandect "0.5.4"]
                 ;; error handling
                 [dire "0.5.4"]
                 ;; data validation and coercion
                 [prismatic/schema "1.1.0"]
                 [camel-snake-kebab "0.4.0"]
                 ;; cassandra / db deps
                 [clojurewerkz/cassaforte "2.0.2"]
                 [net.jpountz.lz4/lz4 "1.3.0"]
                 [org.xerial.snappy/snappy-java "1.1.2"]
                 ;; file system utils
                 [me.raynes/fs "1.4.6"]
                 [com.stuartsierra/component "0.3.1"]
                 [leiningen-core "2.5.3"]
                 [clj-gdal "0.3.4"]]
  :aliases {"lcmap"
            ^{:doc "Command line interface for lcmap.data. For more info, run:
            `lein lcmap --help`"}
            ^:pass-through-help
            ["run" "-m" "lcmap.data.cli"]}
  :repl-options {:init-ns lcmap.data.dev}
  :test-selectors {:default :unit
                   :unit    (complement :integration)
                   :db      :integration
                   :all     (constantly true)}
  :logger [lcmap.data :debug
           lcmap.data.components :debug
           com.datastax :error
           co.paralleluniverse :error
           org.gdal :error]
  :profiles {:dev
             {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                             [slamhound "1.5.5"]]
              :plugins [[lein-kibit "0.1.2"]
                        [lein-codox "0.9.4"]]
              :aliases {"slamhound" ["run" "-m" "slam.hound"]}
              :source-paths ["dev-resources/src"]}})
