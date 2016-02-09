(defproject  gov.usgs.eros/lcmap-data-clj "0.1.0-SNAPSHOT"
  :description "LCMAP data layer management tools"
  :url "http://github.com/USGS-EROS/lcmap-data-clj"
  :license {:name "TBD"
            :url ""}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/tools.logging "0.3.1"]
                 [twig "0.1.2"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [clojurewerkz/cassaforte "2.0.0"]
                 [net.jpountz.lz4/lz4 "1.3.0"]
                 [org.xerial.snappy/snappy-java "1.1.2"]
                 [camel-snake-kebab "0.3.2"]
                 [me.raynes/fs "1.4.6"]
                 [clj-gdal "0.2.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [leiningen-core "2.5.3"]]
  :aliases {"db" ["run" "-m" "lcmap-data-clj.core/cli-main"]}
  :repl-options {:init-ns lcmap-data-clj.dev}
  :test-selectors {:default (complement :integration)
                   :slow    :integration
                   :fast    (complement :integration)
                   :all     (constantly true)}
  :profiles {:dev
             {:env
              {:active-profile "dev"
               ;; Use environment variables for DB configuration:
               ;; LCMAP_HOSTS, LCMAP_USER, LCMAP_PASS
               :db {:hosts []
                    :port 9042
                    :protocol-version 3
                    :spec-keyspace "lcmap"
                    :spec-table "tile_specs"}
               :logger [lcmap-data-clj :debug
                        com.datastax :info
                        co.paralleluniverse :error
                        org.gdal :error]
               :dependencies [[org.clojure/tools.namespace "0.2.11"
                               slamhound "1.5.5"]]
               :aliases {"slamhound" ["run" "-m" "slam.hound"]}
               :plugins [[lein-kibit "0.1.2"]]}}})
