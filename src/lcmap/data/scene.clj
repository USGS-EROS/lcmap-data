(ns lcmap.data.scene
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.core.memoize]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as query]
            [clojure.data.json :as json])
  (:refer-clojure :exclude [find]))

;; How do we decide where to look for scenes?
;; ... is this part of the system config?
;; ... is this part of the tile-spec?
;; ... what happens if we store the same scene projected differently?

;; We have to look in one place...
;; ...similar ttile-specs.

(defn column-names
  ""
  [db]
  (let [session (:session db)
        kn (get-in db [:cfg :lcmap.data :scene-keyspace])
        tn (get-in db [:cfg :lcmap.data :scene-table])
        columns (cql/describe-columns session kn tn)]
    (->> columns
         (map :column_name)
         (map keyword)
         (into []))))

(def column-names-memo
  (clojure.core.memoize/lu column-names))

(defn find
  ""
  [db scene]
  (let [session (:session db)
        kn (get-in db [:cfg :lcmap.data :scene-keyspace])
        tn (get-in db [:cfg :lcmap.data :scene-table])
        scene- (select-keys scene (column-names-memo db))]
    (log/debug "find scene" scene-)
    (cql/use-keyspace session kn)
    (map #(->> % :global_metadata json/read-str)
         (cql/select session tn (query/where scene-)))))

(defn save
  ""
  [db scene]
  (let [session (:session db)
        kn (get-in db [:cfg :lcmap.data :scene-keyspace])
        tn (get-in db [:cfg :lcmap.data :scene-table])
        scene- (select-keys scene (column-names-memo db))]
    (log/debug "save scene" kn tn scene-)
    (cql/use-keyspace session kn)
    (cql/insert-async session tn scene-)))

(defn save-band
  ""
  [db band]
  (let [scene (select-keys band (column-names-memo db))
        global_metadata (band :global_metadata)]
    (log/debug "save-band")
    (save db (assoc scene :global_metadata (json/json-str global_metadata)))))
