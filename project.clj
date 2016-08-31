(defproject  gov.usgs.eros/lcmap-data "1.0.0-SNAPSHOT"
  :parent-project {
    :coords [gov.usgs.eros/lcmap-system "1.0.0-SNAPSHOT"]
    :inherit [
      :deploy-repositories
      :license
      :managed-dependencies
      :plugins
      :pom-addition
      :repositories
      :target-path
      :test-selectors
      ;; XXX The following can be un-commented once this issue is resolved:
      ;;     * https://github.com/achin/lein-parent/issues/3
      ;; [:profiles [:uberjar :dev]]
      ]}
  :description "LCMAP data layer management tools"
  :url "http://github.com/USGS-EROS/lcmap-data"
  :dependencies [[org.clojure/clojure]
                 [org.clojure/core.memoize]
                 [org.clojure/data.xml]
                 [org.clojure/data.zip]
                 [org.clojure/data.json]
                 [org.clojure/tools.cli]
                 ;; checksum (for debug logging)
                 [pandect]
                 ;; error handling
                 [dire]
                 ;; data validation and coercion
                 [prismatic/schema]
                 [camel-snake-kebab]
                 ;; cassandra / db deps
                 [clojurewerkz/cassaforte]
                 [net.jpountz.lz4/lz4]
                 [org.xerial.snappy/snappy-java]
                 ;; file system utils
                 [me.raynes/fs]
                 [com.stuartsierra/component]
                 [leiningen-core]
                 [clj-gdal]
                 [clj-time/clj-time]
                 ;; XXX note that we may still need to explicitly include the
                 ;; Apache Java HTTP client, since the version used by the LCMAP
                 ;; client is more recent than that used by Chas Emerick's
                 ;; 'friend' library (the conflict causes a compile error which
                 ;; is worked around by explicitly including Apache Java HTTP
                 ;; client library).
                 ;; XXX temp dependencies:
                 [org.apache.httpcomponents/httpclient]
                 ;; LCMAP Components
                 [gov.usgs.eros/lcmap-config]
                 [gov.usgs.eros/lcmap-logger]]
  :aliases {"lcmap"
            ^{:doc "Command line interface for lcmap.data. For more info, run:
            `lein lcmap --help`"}
            ^:pass-through-help
            ["run" "-m" "lcmap.data.cli"]}
  :repl-options {:init-ns lcmap.data.dev}
  :main lcmap.data.cli
  :aot [lcmap.data.cli]
  :plugins [[lein-parent "0.3.0"]]
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
  :profiles {
    :uberjar {:aot :all}
    :dev {
      :aliases {"slamhound" ["run" "-m" "slam.hound"]}
      :source-paths ["dev-resources/src"]}
    :test {}})
