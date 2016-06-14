(ns lcmap.data.ccdc
  ""
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as query]
            [lcmap.data.tile :refer [snap]]
            [lcmap.data.tile-spec :as tile-spec]))

(def spec {:tile_x (* 128 30)
           :tile_y (* 128 30)
           :shift_x 0
           :shift_y 0})

(defn find-tile
  [pixel {:keys [x y]} db]
  (let [spec    (first (tile-spec/find db "LCMAP/SEE/CCDC"))
        [tx ty] (snap x y spec)
        session (:session db)
        where   (query/where {:tx tx :ty ty})]
    (cql/use-keyspace session "ccdc")
    (cql/select session "coefficients_v1" where)))
