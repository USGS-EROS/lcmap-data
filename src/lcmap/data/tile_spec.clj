(ns lcmap.data.tile-spec
  "Functions for retrieving and creating tile-specs."
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as query]
            [clojure.tools.logging :as log]
            [gdal.core]
            [gdal.dataset])
  (:refer-clojure :exclude [find]))

(defn column-names
  "Retrieve names of tile-spec columns. Useful for providing a list of values
   from a map that need to be persisted. Cassaforte does not ignore key/values
   when inserting data (quite reasonably)."
  [db]
  (let [session       (:session db)
        spec-keyspace (get-in db [:cfg :lcmap.data :spec-keyspace])
        spec-table    (get-in db [:cfg :lcmap.data :spec-table])
        columns       (cql/describe-columns session spec-keyspace spec-table)]
    (->> columns
         (map :column_name)
         (map keyword)
         (into []))))

(defn find
  "Retrieve a tile spec for given band."
  [db params]
  (let [session       (:session db)
        spec-keyspace (get-in db [:cfg :lcmap.data :spec-keyspace])
        spec-table    (get-in db [:cfg :lcmap.data :spec-table])]
    ;; XXX save ignores param keys that do not correspond to
    ;;     a column, should find do the same?
    (log/debugf "Find tile-spec: %s" params)
    (cql/use-keyspace session spec-keyspace)
    (cql/select session spec-table
                (query/where params)
                (query/allow-filtering))))

(defn save
  "Insert a tile-spec. Ignores key/values that do not correspond
   to a tile-spec table column."
  [db tile-spec]
  (log/debugf "Save tile-spec: %s" tile-spec)
  (let [session       (:session db)
        spec-keyspace (get-in db [:cfg :lcmap.data :spec-keyspace])
        spec-table    (get-in db [:cfg :lcmap.data :spec-table])
        params        (select-keys tile-spec (column-names db))]
      (cql/use-keyspace session spec-keyspace)
      (cql/insert session spec-table params)))

(defn band->spec
  "Take tile-spec properties from a band."
  [db band]
  (let [cs (column-names db)
        ks (map keyword cs)]
    (select-keys band ks)))

(defn dataset->spec
  "Deduce tile spec properties from band's dataset at file_path and band's data_shape"
  [path shape]
  (let [] ;; XXX huh?
    (gdal.core/with-dataset [ds path]
      (let [proj (gdal.dataset/get-projection-str ds)
            [rx px _ ry _ py] (gdal.dataset/get-geo-transform ds)
            [dx dy] shape
            pixel_x (int px)
            pixel_y (int py)
            tile_x  (int (* px dx))
            tile_y  (int (* py dy))
            shift_x (int (mod rx pixel_x))
            shift_y (int (mod ry pixel_y))]
        {:projection proj
         :pixel_x pixel_x
         :pixel_y pixel_y
         :tile_x tile_x
         :tile_y tile_x
         :shift_x shift_x
         :shift_y shift_y}))))
