(defproject  gov.usgs.eros/lcmap-data "0.5.0-SNAPSHOT"
  :description "LCMAP data layer management tools"
  :url "http://github.com/USGS-EROS/lcmap-data"
  :license {:name "NASA Open Source Agreement, Version 1.3"
            :url "http://ti.arc.nasa.gov/opensource/nosa/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.memoize "0.5.9"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.5"]
                 ;; checksum (for debug logging)
                 [pandect "0.6.0"]
                 ;; error handling
                 [dire "0.5.4"]
                 ;; data validation and coercion
                 [prismatic/schema "1.1.2"]
                 [camel-snake-kebab "0.4.0"]
                 ;; cassandra / db deps
                 [clojurewerkz/cassaforte "2.0.2"]
                 [net.jpountz.lz4/lz4 "1.3.0"]
                 [org.xerial.snappy/snappy-java "1.1.2.6"]
                 ;; file system utils
                 [me.raynes/fs "1.4.6"]
                 [com.stuartsierra/component "0.3.1"]
                 [leiningen-core "2.6.1"]
                 [clj-gdal "0.4.0-SNAPSHOT"]
                 [clj-time/clj-time "0.12.0"]
                 ;; XXX note that we may still need to explicitly include the
                 ;; Apache Java HTTP client, since the version used by the LCMAP
                 ;; client is more recent than that used by Chas Emerick's
                 ;; 'friend' library (the conflict causes a compile error which
                 ;; is worked around by explicitly including Apache Java HTTP
                 ;; client library).
                 ;; XXX temp dependencies:
                 [org.apache.httpcomponents/httpclient "4.5.2"]
                 ;; LCMAP Components
                 [gov.usgs.eros/lcmap-config "0.5.0-SNAPSHOT"]
                 [gov.usgs.eros/lcmap-logger "0.5.0-SNAPSHOT"]]
  :aliases {"lcmap"
            ^{:doc "Command line interface for lcmap.data. For more info, run:
            `lein lcmap --help`"}
            ^:pass-through-help
            ["run" "-m" "lcmap.data.cli"]}
  :repl-options {:init-ns lcmap.data.dev}
  :main lcmap.data.cli
  :aot [lcmap.data.cli]
  :plugins [[lein-kibit "0.1.2"]
            [lein-codox "0.9.5"]]
  :codox {
    :project {
      :name "lcmap.data"
      :description "LCMAP Data Library and CLI"}
    :namespaces [#"^lcmap.data\."]
    :output-path "docs/master/current"
    :doc-paths ["docs/source"]
    :metadata {
      :doc/format :markdown
      :doc "Documentation forthcoming"}}

  :test-selectors {:default (complement :integration)
                   :unit    (complement :integration)
                   :db      :integration
                   :all     (constantly true)}
  :profiles {:dev
             {:dependencies [[org.clojure/tools.namespace "0.3.0-alpha3"]
                             [slamhound "1.5.5"]]
              :aliases {"slamhound" ["run" "-m" "slam.hound"]}
              :source-paths ["dev-resources/src"]}
             :test {}})
