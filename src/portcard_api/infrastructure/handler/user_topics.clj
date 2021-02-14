(ns portcard-api.infrastructure.handler.user-topics
  (:require [clojure.spec.alpha :as s]
            [portcard-api.usecase.get-user-topics :as get-user-topics-usecase]
            [portcard-api.usecase.get-latest-user-topics :as get-latest-user-topics-usecase]
            [portcard-api.usecase.post-user-topic :as post-user-topic-usecase]
            [portcard-api.usecase.get-user-topic-capture :as get-user-topic-capture-usecase]
            [portcard-api.usecase.delete-user-topic :as delete-user-topic-usecase]
            [reitit.ring.middleware.multipart :as multipart]
            [spec-tools.data-spec :as ds]
            [clojure.walk :as w]
            [portcard-api.interface.database.utils :as utils]
            [portcard-api.util :as util]
            [portcard-api.domain.user-roles :as user-roles-model]
            [clojure.java.io :as io]
            [portcard-api.domain.errors :as errors]))

;; from int path
;; take int path
;; cetegory string path


(s/def ::from int?)
(s/def ::take int?)
(s/def ::category string?)
(s/def ::get-user-topics-parameters
  (s/keys :req-un [::from ::take] :opt-un [::category]))
(s/def ::user-id string?)
(s/def ::topic-id string?)
(s/def ::get-user-topics-paths (s/keys :req-un [::user-id]))

(s/def ::get-latest-user-topics-parameters
  (s/keys :opt-un [::take]))

(s/def ::get-latest-user-topics-paths (s/keys :req-un [::user-id]))
(s/def ::delete-user-topic-paths (s/keys :req-un [::user-id ::topic-id]))

(s/def ::get-user-topic-capture-paths (s/keys :req-un [::user-id ::topic-id ::image-blob]))
;; topic model body
(s/def ::file multipart/temp-file-part)
(s/def ::title string?)
(s/def ::category string?)
(s/def ::description string?)

(s/def ::uid string?)
(s/def ::idx int?)
(s/def ::image-blob string?)
(s/def ::created_at int?)

(def http-url-regex #"^https?:\/\/[\w\-\.\/\?\,\#\:\u3000-\u30fe\u4e00-\u9fa0\uff01-\uffe3]+")
(def http-url? (partial re-matches http-url-regex))
(s/def ::link (s/and string? http-url?))
;; (s/valid? ::link "https://github.com/MeguruMokke/こんにちは世界")
;;
(s/def ::topic-response (s/keys :req-un [::uid ::idx ::title ::category ::link ::image-blob ::created_at] :opt-un [::description]))
(s/def ::get-user-topics-responses
  (s/coll-of ::topic-response))

(def post-topic-parameters
  {:file multipart/temp-file-part
   :title ::title
   :category ::category
   :link ::link
   (ds/opt :description) ::description})

(defn ->topic [title category description link]
  (util/remove-empty
   {:title title
    :category category
    :link link
    :description description}))

(defn ->topic-response
  [{:keys [^java.util.UUID uid idx title category link description image_blob created_at] :as topic}]
  (let [uid (.toString uid)]
    (util/remove-empty
     {:uid uid
      :idx idx
      :category (user-roles-model/decode-role-category category)
      :link link
      :description description
      :image-blob image_blob
      :created_at created_at})))

(def get-user-topics
  {:summary "get user topics (w some filters)"
   :parameters {:query ::get-user-topics-parameters
                :path ::get-user-topics-paths}
   :responses {201 {:body ::get-user-topics-responses}}
   :handler (fn [{:keys [parameters db]}]
              (let [{:keys [query path]} parameters
                    {:keys [from take category]} query
                    {:keys [user-id]} path
                    [result err] (get-user-topics-usecase/get-user-topics user-id from take category db)]
                (cond
                  (not (nil? err)) err
                  :else {:status 201
                         :body (map ->topic-response (:topics result))})))})

(def post-user-topic
  {:summary "post user topic"
   :swagger {:security [{:Bearer []}]}
   :parameters {:multipart post-topic-parameters}
   :handler (fn [{:keys [parameters headers db image-db]}]
              (let [{{:keys [file title category description]} :multipart} parameters
                    id-token (-> headers w/keywordize-keys :authorization)
                    topic-image (:tempfile file)
                    topic (->topic title category description)
                    [result err] (post-user-topic-usecase/post-user-topic id-token topic topic-image db image-db)]
                {:status 201}))})

;; uid string path


(def delete-user-topic
  {:summary "delete user topic"
   :swagger {:security [{:Bearer []}]}
   :parameters {:path ::delete-user-topic-paths}
   :handler (fn [{:keys [parameters headers db image-db]}]
              (let [{{:keys [topic-id user-id]} :path} parameters
                    id-token (-> headers w/keywordize-keys :authorization)
                    [result err] (delete-user-topic-usecase/delete-user-topic id-token topic-id db image-db)]
                (cond
                  (not (nil? err)) err
                  :else {:status 200})))})

;; stats
(def get-user-topics-latest
  {:summary "get user latest topics"
   :parameters {:path ::get-latest-user-topics-paths
                :query ::get-latest-user-topics-parameters}
   :responses {201 {:body ::get-user-topics-responses}}
   :handler (fn [{:keys [parameters header db]}]
              (let  [{:keys [query path]} parameters
                     {:keys [take]} query
                     take (if (nil? take) 1 take)
                     {:keys [user-id]} path
                     [result err] (get-latest-user-topics-usecase/get-latest-user-topics user-id take db)]
                (cond
                  (not (nil? err)) err
                  :else {:status 201
                         :body (map ->topic-response (:topics result))})))})

(def get-user-topic-capture
  {:summary "get user topic capture image"
   :parameters {:path ::get-user-topic-capture-paths}
   :handler (fn [{:keys [parameters db image-db]}]
              (let [{{:keys [user-id topic-id image-blob]} :path} parameters
                    topic-id (java.util.UUID/fromString topic-id)
                    [{{:keys [file]} :image} err] (get-user-topic-capture-usecase/get-user-topic-capture user-id topic-id image-blob db image-db)
                    [input-stream input-stream-err] (try [(io/input-stream file) nil]
                                                         (catch Exception e [nil (errors/unknown-error (.getMessage e))]))]
                (cond
                  (not (nil? err)) err
                  (not (nil? input-stream-err)) input-stream-err
                  :else {:status 200
                         :headers {"Content-type" "image/png"}
                         :body (io/input-stream file)})))})
