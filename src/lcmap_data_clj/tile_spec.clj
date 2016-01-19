(ns lcmap-data-clj.tile-spec
  (:require [gdal.dataset :as gd]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as cq]
            [clojurewerkz.cassaforte.utils :as cu]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [lcmap-data-clj.util :as util]
            [clojure.tools.logging :as log]
            :reload))

(defn validate
  "Ensure a tile-spec values meet basic criteria."
  ([spec]
   (let [rules {:keyspace-name  string?
                :table-name     string?
                :projection     string?
                :tile-x         (every-pred integer? pos?)   ; number of pixels wide
                :tile-y         (every-pred integer? pos?)   ; number of pixels tall
                :pixel-x        number?                      ; west-east in projection units
                :pixel-y        number?                      ; north-south in projection units
                :shift-x        number?                      ; west-east offset in proj. units
                :shift-y        number?                      ; north-south offset in proj. units
                :bands          (partial every? :ubid)}]     ; list of maps containing :ubid
   (validate spec rules)))
  ([spec rules]
   (remove nil? (for [[field rule] rules]
                  (if-not (rule (spec field))
                    {field :invalid})))))

(defn find
  "Retrieve a tile spec for given band"
  [band system]
  (let [session  (-> system :database :session)
        keyspace (-> system :config :db :spec-keyspace)
        table    (-> system :config :db :spec-table)
        params   (cq/where (transform-keys ->snake_case band))
        results  (cql/select session table params (cq/allow-filtering))]
    (log/debug "Find bands" band)
    (map #(transform-keys (util/ifa keyword? ->kebab-case) %) results)))

(defn save
  "Insert a tile-spec into the tile_specs table"
  [tile-spec system]
  (let [session (-> system :database :session)
        maybe-snake (util/ifa keyword? ->snake_case)
        spec-table (-> system :config :db :spec-table)
        spec-keyspace (-> system :config :db :spec-keyspace)
        spec-snaked (transform-keys maybe-snake tile-spec)]
    (log/debug "Save tile-spec" spec-snaked)
    (cql/insert session "tile_specs" spec-snaked)))
