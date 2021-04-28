(ns portcard-api.infrastructure.gstorage.image-db
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [taoensso.timbre :as timbre]
            [portcard-api.infrastructure.gstorage.utils :as utils-gstorage]
            [clojure.spec.alpha :as s]
            [portcard-api.interface.database.utils :as utils])
  (:import [javax.imageio ImageIO]
           [com.google.cloud.storage Bucket Storage Storage$BucketGetOption StorageException StorageOptions
            Storage$BucketListOption
            Storage$BlobListOption]))

;; data types
(s/def ::bucket-addresses map?)
(s/def ::gcs-image-db-boundary (s/keys :req-un [::storage ::bucket-addresses]))
(defrecord GCSImageDBBoundary [storage bucket-addresses])

;; constructor spec
(s/fdef ->GCSImageDBBoundary
  :args (s/cat ::storage ::utils-gstorage/storage ::bucket-addresses ::bucket-addresses)
  :ret ::gcs-image-db-boundary)

;; initializer
(s/fdef init
  :args (s/cat :bucket-names ::bucket-names  :storage (s/? ::storage))
  :ret (s/or :success (s/tuple ::gcs-image-db-boundary nil?)
             :failure (s/tuple nil? string?)))

(defn init
  "initizer of Google Cloud Storage Accessor"
  ([bucket-addresses]
   (init bucket-addresses (utils-gstorage/get-storage)))
  ([bucket-addresses storage]
   (if (every?
        (fn [bucket-name] (-> (utils-gstorage/get-bucket storage bucket-name) nil? not))
        (vals bucket-addresses))
     [(->GCSImageDBBoundary storage bucket-addresses) nil]
     [nil (format "cannot initialize gcs-image-db-boundary: the bucket \"%s\"cannot accessable" bucket-addresses)])))

(defn gcs-image-boundary [env]
  (let [bucket-names {:icons (:gcs-icons-bucket-name env)
                      :topic-captures (:gcs-captures-bucket-name env)}
        [boundary err] (init bucket-names)]
    (if err (throw (ex-info err {}))
        boundary)))

(defmethod ig/init-key ::image-db
  [_ {:keys [env]}]
  (timbre/info "connect google cloud storage for image db")
  (gcs-image-boundary env))

;; (let [{:keys [storage bucket-addresses]}
;;       (gcs-image-boundary
;;        {:gcs-captures-bucket-name "portcard-captures"
;;         :gcs-icons-bucket-name "portcard-icons"})]
;;   (->
;;    (.list storage
;;           (:icons bucket-addresses)
;;           (into-array [(Storage$BlobListOption/pageSize 100)
;;                        ;; (Storage$BucketListOption/pageSize 100)
;;                        ]))
;;    .iterateAll
;;    .iterator
;;    iterator-seq))

;; (let [{:keys [storage bucket-names]} (first (init ["portcard-captures" "portcard-icons"]))]
;;   ;; (iterator-seq
;;   ;;  (.iterator
;;   ;;   (.iterateAl
;;   ;;    (.list storage
;;   ;;           (into-array [(Storage$BucketListOption/pageSize 100)]))))
;;   ;;  )
;;   (list
;;    (take 3
;;          (iterator-seq
;;           (.iterator
;;            (.iterateAll
;;             (.list storage (first bucket-names) (into-array [(Storage$BlobListOption/pageSize 100)]))))))))
