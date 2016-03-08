(defproject  gov.usgs.eros/lcmap-data "0.0.2-SNAPSHOT"
  :description "LCMAP data layer management tools"
  :url "http://github.com/USGS-EROS/lcmap-data-clj"
  :license {:name "NASA Open Source Agreement, Version 1.3"
            :url "http://ti.arc.nasa.gov/opensource/nosa/"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/tools.cli "0.3.3"]
                 [twig "0.1.4"]
                 [dire "0.5.4"]
                 [clojurewerkz/cassaforte "2.0.0"]
                 [net.jpountz.lz4/lz4 "1.3.0"]
                 [org.xerial.snappy/snappy-java "1.1.2"]
                 [camel-snake-kebab "0.3.2"]
                 [me.raynes/fs "1.4.6"]
                 [clj-gdal "0.2.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [leiningen-core "2.5.3"]]
  :aliases {"lcmap"
            ^{:doc "Command line interface for lcmap.data. For more info, run:
            `lein lcmap --help`"}
            ^:pass-through-help
            ["run" "-m" "lcmap.data.cli"]}
  :repl-options {:init-ns lcmap.data.dev}
  :test-selectors {:default (complement :integration)
                   :slow    :integration
                   :fast    (complement :integration)
                   :all     (constantly true)}
  :profiles {
    :dev {
      :dependencies [[org.clojure/tools.namespace "0.2.11"]
                     [pandect "0.5.4"]
                     [slamhound "1.5.5"]]
      :plugins [[lein-kibit "0.1.2"]]
      :aliases {"slamhound" ["run" "-m" "slam.hound"]}
      :env
       {:active-profile "dev"
        ;; Use environment variables for DB configuration:
        ;; LCMAP_HOSTS, LCMAP_USER, LCMAP_PASS
        :db {:hosts []
             :port 9042
             :protocol-version 3
             :spec-keyspace "lcmap"
             :spec-table "tile_specs"}
        :logger [lcmap.dataj :info
                 com.datastax :error
                 co.paralleluniverse :error
                 org.gdal :error]}}})
