(ns portcard-api.usecase.post-user-topic
  (:require [portcard-api.util :refer [err->> border-error rand-str]]
            [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.image-db.topic-captures-repository :as topic-captures-repository]
            [portcard-api.domain.errors :as errors]
            [portcard-api.interface.image-processor.core :as image-processor]
            [portcard-api.domain.images :as images-domain]
            [portcard-api.interface.database.user-topics-repository :as user-topics-repository]
            [portcard-api.domain.user-topics :as user-topic-model]
            [portcard-api.domain.user-roles :as user-roles-model]
            [taoensso.timbre :as timbre]
            [portcard-api.util :as util]))

(def max-topic-image-size 512)

(defn check-id-token [{:keys [id-token db] :as m}]
  (let [{:keys [result user-id cause]} (safe-decode-token id-token)]
    (if (= :success result)
      [(assoc m :user-id user-id) nil]
      [nil cause])))

(defn check-user-exist [{:keys [user-id db] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uid user-id)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? user) [nil errors/user-not-found]
      (:is_deleted user) [nil errors/user-is-deleted]
      :else [(-> m (assoc :user user)) nil])))

(defn ->topic-image-buffer [{:keys [user-id topic topic-image-stream] :as m}]
  (try [(assoc m :topic-image-buffer
               (image-processor/->climb-image topic-image-stream max-topic-image-size))
        nil]
       (catch Exception e
         (timbre/warn "invalid image is uploaded from " user-id ". topic: " topic)
         [nil (errors/unknown-error (.getMessage e))])))

(defn insert-topic-image [{:keys [topic-image-buffer image-db] :as m}]
  (let [topic-image-blob (str (rand-str images-domain/icon-blob-length) ".png")
        [status err] (err->> {:function #(topic-captures-repository/insert-capture image-db topic-image-buffer topic-image-blob)
                              :error-wrapper errors/database-error}
                             border-error)]
    (cond
      (not (nil? err)) [nil err]
      (not status) [nil errors/icon-save-failed]
      :else
      [(assoc m :topic-image-blob topic-image-blob) nil])))

(defn ->user-topic [{:keys [user topic topic-image-blob] :as m}]
  (let [user-uid (:uid user)
        {:keys [title category link description]} topic
        category (user-roles-model/role-category category)
        uid (java.util.UUID/randomUUID)
        topic (util/remove-empty
               {:uid uid
                :user_uid user-uid
                :title title
                :link link
                :description description
                :category category
                :image_blob topic-image-blob})]
    [(assoc m :topic topic) nil]))

(defn insert-topic [{:keys [db topic] :as m}]
  (let [[status err] (err->> {:function #(user-topics-repository/create-user-topic db topic)
                              :error-wrapper errors/database-error}
                             border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else [(assoc m :result status) nil])))

(defn close-topic-image-stream [{:keys [topic-image-stream] :as m}]
  (try
    (.close topic-image-stream)
    [m nil]
    (finally [m nil])))

(defn post-user-topic [id-token topic topic-image-stream db image-db]
  (err->>
   {:id-token id-token
    :db db
    :topic topic
    :topic-image-stream topic-image-stream
    :image-db image-db}
   check-id-token
   check-user-exist
   ->topic-image-buffer
   insert-topic-image
   ->user-topic
   insert-topic
   close-topic-image-stream))
