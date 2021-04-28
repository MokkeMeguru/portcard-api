(ns portcard-api.interface.image-db.icons-repository
  (:require [clojure.java.io :as io]
            [portcard-api.infrastructure.image-db.image-db]
            [portcard-api.interface.database.utils :as utils]
            [portcard-api.interface.image-db.utils :refer [file-extension]]
            [portcard-api.infrastructure.gstorage.image-db]
            [portcard-api.infrastructure.image-db.image-db]
            [portcard-api.interface.image-db.gstorage-utils :as gstorage-utils]
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

(defprotocol Icons
  (get-icons [db])
  (insert-icon [db ^BufferedImage image blob])
  (get-icon [db blob])
  (delete-icon [db blob]))

(defn icons-repository? [inst]
  (satisfies? Icons inst))

;; (.isFile (io/file "image-db" "icons" "Mnx4I[pCFOoIggj.png"))

(extend-protocol Icons
  portcard_api.infrastructure.image_db.image_db.LocalImageBoundary
  (get-icons [{:keys [spec]}]
    {:files (filter #(-> % str file-extension (= ".png")) (file-seq (io/file (:parent spec) "icons")))})
  (insert-icon [{:keys [spec]} image blob]
    (timbre/info "image-db/query save icon file "  blob)
    (ImageIO/write image "png" (io/file (:parent spec) "icons" blob)))
  (get-icon [{:keys [spec]} blob]
    (timbre/info "image-db/query get file " blob)
    (let [file (io/file (:parent spec) "icons" blob)]
      (if (.isFile file)
        (do
          (timbre/info "query accepted")
          {:image-stream (io/input-stream file)})
        {})))
  (delete-icon [{:keys [spec]} blob]
    (timbre/info "image-db/query delete the file " blob)
    (let [file (io/file (:parent spec) "icons" blob)]
      (if (.isFile file)
        (do (io/delete-file file)
            true)
        false))))

;; (defonce inst (portcard-api.infrastructure.image-db.image-db/->LocalImageBoundary {:parent "image-db"}))

;; (get-icons inst)

(extend-protocol Icons
  portcard_api.infrastructure.gstorage.image_db.GCSImageDBBoundary
  (get-icons [{:keys [storage bucket-addresses]}]
    (take
     3
     (->
      (.list storage (:icons bucket-addresses) (into-array [(Storage$BlobListOption/pageSize 10)]))
      .iterateAll
      .iterator
      iterator-seq)))

  (insert-icon [{:keys [storage bucket-addresses]} ^BufferedImage image-buffer blob]
    (let [temp-file (java.io.File/createTempFile "temp-icon" ".png")
          storage-blob-write-options (make-array Storage$BlobWriteOption 0)
          blob-id (BlobId/of (:icons bucket-addresses) blob)
          blob-info (-> (BlobInfo/newBuilder blob-id) (.setContentType "image/png") .build)]
      (when (and temp-file (ImageIO/write image-buffer "png" temp-file))
        (let [status (with-open [from (-> temp-file FileInputStream. .getChannel)
                                 to (.writer storage blob-info storage-blob-write-options)]
                       (ByteStreams/copy from to))]
          (.delete temp-file)
          (timbre/info (format "save the image %s: status %s" blob status))
          (if status blob (throw (ex-info (format "save the image %s into GCS failed" blob) {})))))))

  (get-icon [{:keys [storage bucket-addresses]} blob]
    (let [gcs-blob (.get storage (BlobId/of (:icons bucket-addresses) blob))
          blob-source-option (make-array Blob$BlobSourceOption 0)]
      (if (and gcs-blob (.exists gcs-blob blob-source-option))
        (do
          (timbre/info "load image successed ... %s" gcs-blob)
          {:image-stream (gstorage-utils/blob->input-stream gcs-blob)})
        (timbre/info (format "load image failed ... %s" gcs-blob)))))

  (delete-icon [{:keys [storage bucket-addresses]} blob]
    (let [gcs-blob (.get storage (BlobId/of (:icons bucket-addresses) blob))
          blob-source-option (make-array Blob$BlobSourceOption 0)]
      (when (and gcs-blob (.exists gcs-blob blob-source-option))
        (.delete gcs-blob blob-source-option)))))

;; (defonce inst
;;   (portcard-api.infrastructure.gstorage.image-db/gcs-image-boundary
;;    {:gcs-captures-bucket-name "portcard-captures"
;;     :gcs-icons-bucket-name "portcard-icons"}))

;; (get-icon inst (.getName (first (get-icons inst))))
;; (insert-icon inst (ImageIO/read (io/file (io/resource "sample.png"))) "sample2.png")
;; (get-icon inst "sample2.png")
;; (delete-icon inst "sample2.png")
;; (get-icon inst "sample2.png")
