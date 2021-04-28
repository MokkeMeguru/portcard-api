(ns portcard-api.interface.image-db.topic-captures-repository
  (:require [clojure.java.io :as io]
            [portcard-api.infrastructure.gstorage.image-db]
            [portcard-api.infrastructure.image-db.image-db]
            [portcard-api.interface.image-db.gstorage-utils :as gstorage-utils]
            [portcard-api.interface.image-db.utils :refer [file-extension]]
            [taoensso.timbre :as timbre])
  (:import [javax.imageio ImageIO]
           [java.awt.image BufferedImage]
           [java.io FileInputStream]
           [com.google.common.io ByteStreams]
           [com.google.cloud.storage
            Blob
            BlobId
            BlobInfo
            Blob$BlobSourceOption
            Storage$BlobWriteOption
            StorageOptions
            Storage$BucketGetOption
            Storage$BucketListOption
            Storage$BlobListOption]))

(defprotocol TopicCaptures
  (get-captures [db])
  (get-capture [db blob])
  (insert-capture [db ^BufferedImage image blob])
  (delete-capture [db blob]))

(defn topic-captures-repository? [inst]
  (satisfies? TopicCaptures inst))

(extend-protocol TopicCaptures
  portcard_api.infrastructure.image_db.image_db.LocalImageBoundary
  (get-captures [{:keys [spec]}]
    {:files (filter #(-> % str file-extension (= ".png")) (file-seq (io/file (:parent spec) "captures")))})
  (insert-capture [{:keys [spec]} image blob]
    (ImageIO/write image "png" (io/file (:parent spec) "captures" blob)))
  (get-capture [{:keys [spec]} blob]
    (let [file (io/file (:parent spec) "captures" blob)]
      (if (.isFile file)
        {:image-stream (io/input-stream file)}
        {})))
  (delete-capture [{:keys [spec]} blob]
    (let [file (io/file (:parent spec) "captures" blob)]
      (if (.isFile file)
        (do (io/delete-file file)
            true)
        false))))

(extend-protocol TopicCaptures
  portcard_api.infrastructure.gstorage.image_db.GCSImageDBBoundary
  (get-captures [{:keys [storage bucket-addresses]}]
    (take
     3
     (->
      (.list storage (:topic-captures bucket-addresses) (into-array [(Storage$BlobListOption/pageSize 10)]))
      .iterateAll
      .iterator
      iterator-seq)))

  (insert-capture [{:keys [storage bucket-addresses]} ^BufferedImage image-buffer blob]
    (let [temp-file (java.io.File/createTempFile "temp-capture" ".png")
          storage-blob-write-options (make-array Storage$BlobWriteOption 0)
          blob-id (BlobId/of (:topic-captures bucket-addresses) blob)
          blob-info (-> (BlobInfo/newBuilder blob-id) (.setContentType "image/png") .build)]
      (when (and temp-file (ImageIO/write image-buffer "png" temp-file))
        (let [status (with-open [from (-> temp-file FileInputStream. .getChannel)
                                 to (.writer storage blob-info storage-blob-write-options)]
                       (ByteStreams/copy from to))]
          (.delete temp-file)
          (timbre/info (format "save the image %s: status %s" blob status))
          (if status blob (throw (ex-info (format "save the image %s into GCS failed" blob) {})))))))

  (get-capture [{:keys [storage bucket-addresses]} blob]
    (let [gcs-blob (.get storage (BlobId/of (:topic-captures bucket-addresses) blob))
          blob-source-option (make-array Blob$BlobSourceOption 0)]
      (if (and gcs-blob (.exists gcs-blob blob-source-option))
        {:image-stream (gstorage-utils/blob->input-stream gcs-blob)}
        nil)))

  (delete-capture [{:keys [storage bucket-addresses]} blob]
    (let [gcs-blob (.get storage (BlobId/of (:topic-captures bucket-addresses) blob))
          blob-source-option (make-array Blob$BlobSourceOption 0)]
      (when (and gcs-blob (.exists gcs-blob blob-source-option))
        (.delete gcs-blob blob-source-option)))))

;; (defonce inst
;;   (portcard-api.infrastructure.gstorage.image-db/gcs-image-boundary
;;    {:gcs-captures-bucket-name "portcard-captures"
;;     :gcs-icons-bucket-name "portcard-icons"}))

;; (get-capture inst (.getName (first (get-captures inst))))
;; (insert-capture inst (ImageIO/read (io/file (io/resource "sample.png"))) "sample2.png")
;; (get-capture inst "sample2.png")
;; (delete-capture inst "sample2.png")
;; (get-capture inst "sample2.png")
