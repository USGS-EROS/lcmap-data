(ns lcmap.data.tile
  "Functions for retrieving tiles from the DB or producing tiles from
  a band of GDAL data."
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as query]
            [clojure.tools.logging :as log]
            [lcmap.data.tile-spec :as tile-spec]
            [gdal.band])
  (:refer-clojure :exclude [find])
  (:gen-class))

;;; Helper functions

(defn snap
  "Transform an arbitrary projection system coordinate (x,y) into the
   coordinate of the tile that contains it."
  [x y spec]
  (let [{:keys [tile_x tile_y shift_x shift_y]} spec
        tx (- x (mod (+ x shift_x) tile_x))
        ty (- y (mod (+ y shift_y) tile_y))]
    (log/debug "Snap: (%d,%d) to (%d,%d)" x y tx ty)
    [(long tx) (long ty)]))

;;; Database functions

(defn find
  "Query DB for all tiles that match the UBID, contain (x,y), and
   were acquired during a certain period of time."
  [db {:keys [ubid x y acquired] :as tile}]
  (let [spec     (first (tile-spec/find db {:ubid ubid}))
        session  (:session db)
        keyspace (:keyspace_name spec)
        table    (:table_name spec)
        [tx ty]  (snap x y spec)
        [t1 t2]  acquired
        where    (query/where [[= :ubid ubid]
                               [= :x tx]
                               [= :y ty]
                               [>= :acquired (str t1)]
                               [<= :acquired (str t2)]])]
    (log/debug "find tiles" ubid x y acquired)
    (cql/use-keyspace session keyspace)
    (cql/select session table where)))

(defn save
  "Insert tile."
  ([db tile]
   (let [ubid      (select-keys tile [:ubid])
         spec      (first (tile-spec/find db {:ubid ubid}))
         keyspace  (:keyspace_name spec)
         table     (:table_name spec)]
     (log/debug ubid spec keyspace table)
     (save keyspace table tile)))
  ([db keyspace table tile]
   (let [session (get-in db [:session])]
     (log/debug "save tile" tile)
     (cql/use-keyspace session keyspace)
     (cql/insert session table tile)
     tile)))

;;; Dataset functions

(defrecord Tile [x y data])

(defprotocol Tiled
  ""
  (shape [data] "Dimensions [cols rows] of data.")
  (steps [data step-x step-y] "Subsetting coordinates within data.")
  (tiles [data step-x step-y] "List of maps with x, y, data."))

(extend-type org.gdal.gdal.Band
  Tiled
  (shape [band]
    (gdal.band/get-size band))
  (steps [band step-x step-y]
    (let [[x-size y-size] (shape band)]
      (for [x (range 0 x-size step-x)
            y (range 0 y-size step-y)]
        [x y step-x step-y])))
  (tiles [band step-x step-y]
    (for [[x y xs ys] (steps band step-x step-y)]
      (->Tile x y (gdal.band/read-raster-direct band x y xs ys)))))
