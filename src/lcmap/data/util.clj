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
  (when-let [entry (.getNextEntry archive)]
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
               archive (.createArchiveInputStream (new ArchiveStreamFactory) src-stream)]
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
           cis (.createCompressorInputStream csf src-stream)]
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
