(ns portcard-api.infrastructure.gstorage.utils
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as timbre])
  (:import [com.google.cloud.storage Bucket Storage Storage$BucketGetOption StorageException StorageOptions]))

;; data specs
(s/def ::storage-options (partial instance? StorageOptions))
(s/def ::storage (partial instance? Storage))
(s/def ::bucket-name string?)
(s/def ::bucket (partial instance? Bucket))



;; methods spec


(s/fdef get-storage
  :args (s/cat :storage-options (s/? ::storage-options))
  :ret ::storage)

(s/fdef get-bucket
  :args (s/cat :storage ::storage :bucket-name ::bucket-name)
  :ret (s/or :exist ::bucket
             :not-exist empty?))

;; implements
(defn get-storage
  "get storage using the environment variable GOOGLE_APPLICATION_CREDENTIALS

  Parameters:

  - storage-options Please refer the docs of com.google.cloud.storage.StorageOptions

  Notice:

  Please run under the environment which run below command.

      export GOOGLE_APPLICATION_CREDENTIALS=\"<path-to-your-secret-json>\"

  References:

  - https://cloud.google.com/storage/docs/reference/libraries#setting_up_authentication
  "
  ([]
   (get-storage (StorageOptions/getDefaultInstance)))
  ([storage-options]
   {:pre [(instance? StorageOptions storage-options)]}
   (.getService storage-options)))

(defn get-bucket
  "get bucket of storage"
  [storage bucket-name]
  {:pre [(s/valid? ::storage storage) (s/valid? ::bucket-name bucket-name)]
   :post [(or ::bucket nil?)]}
  (try
    (-> storage (.get bucket-name (make-array Storage$BucketGetOption 0)))
    (catch StorageException e
      (timbre/error (.getMessage e)))))
