;;;; LCMAP Data util namespace
;;;;
;;;; This namespace defines a few utility type functions:
;;;; 1. Working with compressed archives (.tar.gz, etc...)
;;;; 2. Meddling with projection information.
;;;;
;;;; Eventually, these functions may find there way out of
;;;; this namespace into a more specific one.
(ns lcmap.data.util
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [gdal.core :as core]
            [gdal.dataset :as gd]
            [leiningen.core.project :as lein-prj] )
  (:import [org.gdal.gdal gdal]
           [org.gdal.osr SpatialReference]
           [org.apache.commons.compress.archivers
            ArchiveInputStream ArchiveStreamFactory]
           [org.apache.commons.compress.compressors
            CompressorInputStream CompressorStreamFactory]))


;;; Compressed Archives Utilities

(defn entries
  "Lazily retrieve a list of archive entries."
  [archive]
  (when-let [entry (. archive getNextEntry)]
    (cons entry (lazy-seq (entries archive)))))

(defn create-entry
  "Creates a file at dest from entry in archive."
  [archive entry dest]
  (let [{:keys [:name :file]} (bean entry)
        output-file (fs/file dest name)]
    (cond file (do (-> output-file fs/parent fs/mkdirs)
                   (io/copy archive output-file)))))

(defn unarchive
  "Unpacks archive entries in file at src into dest directory.

  This handles archives, multiple files represented as a single file,
  for example, a tar file."
  ([src]
   (unarchive src (fs/file (fs/base-name src true))))
  ([src dest]
   (with-open [src-stream (io/input-stream src)
               archive (. (new ArchiveStreamFactory) createArchiveInputStream src-stream)]
     (doseq [entry (entries archive)]
       (create-entry archive entry dest)))
   dest))

(defn uncompress
  "Applies decompression function to file at src into dest file.

  This handles compressed files (e.g. gz, xz, bz2) but not archived files
  (e.g. tar, cpio).
  "
  ([src]
   (uncompress src (fs/file (fs/base-name src true))))
  ([src dest]
   (with-open [src-stream (io/input-stream src)
               dest-stream (io/output-stream dest)]
     (let [csf (new CompressorStreamFactory)
           cis (. csf createCompressorInputStream src-stream)]
       (io/copy cis dest-stream)))
   dest))

(defmacro with-temp
  "Temporarily uncompress and unarchive file at path.

  Provide a binding for the temporary directory to use in
  body"
  [[binding path] & body]
  `(let [tf# (fs/temp-file "lcmap-")
         td# (fs/temp-dir "lcmap-")]
     (try
      (log/debug "Uncompressing" ~path "to" (.getAbsolutePath td#))
      (uncompress ~path tf#)
      (unarchive tf# td#)
      (let [~binding td#]
        (do ~@body))
      (finally
        (log/debug "Cleaning up" td#)
        (fs/delete tf#)
        (fs/delete-dir td#)))))

;;; Projection related utilities

(defn get-proj-from-epsg
  "Build a SpatialReference from an EPSG code"
  [code]
  (let [proj (new SpatialReference)]
    (. proj ImportFromEPSG code)
    proj))

(defn get-proj-from-spec
  "Build a SpatialReference from a spec's projection"
  [spec]
  (if-let [proj (:projection spec)]
    (new SpatialReference proj)))

(defn get-proj-from-wkt
  "Build a SpatialReference from a well-known text string"
  [wkt]
  (let [proj (new SpatialReference wkt)]
    proj))

(defn get-proj-from-image
  "Build a SpatialReference for a GDAL dataset"
  [dataset]
  (let [proj (new SpatialReference (gd/get-projection-str dataset))]
    proj))

(defn get-gdal-meta-map
  "Generate a tile-spec like map for an image."
  [dataset]
  (let [[ux sx _ uy _ sy] (gd/get-geo-transform dataset)
        projection (gd/get-projection-str dataset)]
    {:upper_x ux
     :upper_y uy
     :pixel_x sx
     :pixel_y sy
     :shift_x (mod ux sx)
     :shift_y (mod uy sy)
     :projection projection}))

(defn get-ubid-map
  ""
  [band]
  {:ubid (apply str (interpose "/" ((juxt :satellite :instrument :band_name) band)))})


(defn same-projection?
  "Compare two projections for equivalence"
  [p1 p2]
  (= 1 (. p1 IsSame p2)))

(defprotocol Equivalent
  (equiv [x y]))

(extend-type SpatialReference
  Equivalent
    (equiv [x y]
      (same-projection? x y)))

(def conus-wkt
      "PROJCS[\"Albers\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,
      AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0],
      UNIT[\"degree\",0.0174532925199433],AUTHORITY[\"EPSG\",\"4326\"]],
      PROJECTION[\"Albers_Conic_Equal_Area\"],PARAMETER[\"standard_parallel_1\",29.5],
      PARAMETER[\"standard_parallel_2\",45.5],PARAMETER[\"latitude_of_center\",23],
      PARAMETER[\"longitude_of_center\",-96],PARAMETER[\"false_easting\",0],
      PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]]]")

;;; Buffer Utilities
;;; All of the buffer libraries I came across really make it hard to do what I want:
;;; I just want to be able to treat a buffer like a darn sequence.

(defn buffer-seq
  ""
  [buffer]
  (when (. buffer hasRemaining)
    (cons (. buffer get) (lazy-seq (buffer-seq buffer)))))

;;; General purpose higher order functions.

(defn ifa
  "produces fn if p then f"
  [p f]
  (fn [x]
    (if (p x) (f x) x)))

;;; Load environment settings from lein

(defn get-config
  "Load lein env map"
  []
  (log/debug "Loading LCMAP configuration")
  (:env (lein-prj/read)))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))
