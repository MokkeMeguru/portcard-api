(ns portcard-api.domain.gcs
  (:require [clojure.spec.alpha :as s])
  (:import [com.google.cloud.storage
            Blob$BlobSourceOption
            BlobId
            BlobInfo
            Bucket
            Bucket$Builder
            BucketInfo Storage Storage$BlobListOption Storage$BlobWriteOption
            Storage$BucketGetOption
            Storage$BucketListOption
            StorageException
            StorageOptions]
           [com.google.common.io ByteStreams]

           [java.io InputStream]
           [java.nio.channels Channels]))

(s/def ::prefix (s/and string? #(< 3 (count %))))
(s/def ::suffix (s/and string?))
(s/def ::temp-file-config (s/keys :req-un [::prefix ::suffix]))


;; (s/def ::storage-options (partial instance? StorageOptions))
;; (s/def ::storage (partial instance? Storage))
;; (s/def ::bucket-name string?)
;; (s/def ::bucket (partial instance? Bucket))

;; (s/def ::gcs-image-db-boundary (s/keys :req-un [::storage ::bucket-name]))
