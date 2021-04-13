(ns portcard-api.domain.gcs
  (:require [clojure.spec.alpha :as s])
  (:import (com.google.common.io ByteStreams)
           (java.io InputStream)
           (com.google.cloud.storage
            BlobId
            Storage$BlobListOption
            BlobInfo
            Storage$BlobWriteOption
            Blob$BlobSourceOption
            Bucket BucketInfo Storage StorageOptions
            Storage$BucketGetOption
            Storage$BucketListOption
            Bucket$Builder
            StorageException)

           (com.google.common.io ByteStreams)
           (java.nio.channels Channels)))

(s/def :temp-file/prefix (s/and string? #(< 3 (count %))))
(s/def :temp-file/suffix (s/and string?))
(s/def ::temp-file-config (s/keys :req-un [:temp-file/prefix :temp-file/suffix]))


;; (s/def ::storage-options (partial instance? StorageOptions))
;; (s/def ::storage (partial instance? Storage))
;; (s/def ::bucket-name string?)
;; (s/def ::bucket (partial instance? Bucket))

;; (s/def ::gcs-image-db-boundary (s/keys :req-un [::storage ::bucket-name]))
