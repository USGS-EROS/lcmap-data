(ns lcmap.data.fixtures
  (:require [leiningen.core.project :as lein-prj]
            [lcmap.data.shared-test :as shared]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.query :as query]))

(defn create-tile-specs
  ""
  [{:keys [session spec-keyspace spec-table] :as db}]
  (let [shared {:keyspace_name spec-keyspace
                :table_name "conus"
                :data_shape [128 128]
                :pixel_x 30
                :pixel_y -30
                :tile_x (* 30 128)
                :tile_y (* -30 128)
                :shift_x 15
                :shift_y -15}]
    (cql/use-keyspace session :lcmap_test)
    (cql/insert session :tile_specs (merge shared {:ubid "test/1"}))
    (cql/insert session :tile_specs (merge shared {:ubid "test/2"}))
    (cql/insert session :tile_specs (merge shared {:ubid "test/3"}))))

(defn delete-tile-specs
  ""
  [{:keys [session spec-keyspace spec-table] :as db}]
  (cql/use-keyspace session spec-keyspace)
  (cql/delete session spec-table (query/where {:keyspace_name spec-keyspace :table_name "conus"})))

(defn tile-spec-fixtures
  [f]
  (create-tile-specs (-> shared/test-system :database))
  (f)
  (delete-tile-specs (-> shared/test-system :database)))
