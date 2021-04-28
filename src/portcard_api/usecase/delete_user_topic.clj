(ns portcard-api.usecase.delete-user-topic
  (:require [portcard-api.domain.errors :as errors]
            [portcard-api.interface.database.user-topics-repository :as user-topics-repository]
            [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
            [portcard-api.interface.image-db.topic-captures-repository :as topic-captures-repository]
            [portcard-api.util :refer [border-error err->> rand-str]]))

(defn check-id-token [{:keys [id-token db] :as m}]
  (let [{:keys [result user-id cause]} (safe-decode-token id-token)]
    (if (= :success result)
      [(assoc m :user-id user-id) nil]
      [nil cause])))

(defn check-topic-owner [{:keys [user-id topic-id db] :as m}]
  (let [topic-id (java.util.UUID/fromString topic-id)
        [topic err] (err->>
                     {:function #(user-topics-repository/get-user-topic-by-id db topic-id)
                      :error-wrapper errors/database-error}
                     border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? topic) [nil errors/topic-not-found]
      (not= user-id (:user_uid topic)) [nil errors/invalid-user-operation]
      :else [(assoc m :topic topic) nil])))

(defn delete-topic [{:keys [user-id topic-id db] :as m}]
  (let [topic-id (java.util.UUID/fromString topic-id)
        [status err] (err->>
                      {:function #(user-topics-repository/delete-user-topic db topic-id)
                       :error-wrapper errors/database-error}
                      border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else [m nil])))

(defn delete-topic-image [{:keys [topic image-db] :as m}]
  (let [topic-image-blob (:image_blob topic)
        [status err] (err->>
                      {:function #(topic-captures-repository/delete-capture image-db topic-image-blob)
                       :error-wrapper errors/database-error}
                      border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else [m nil])))

(defn delete-user-topic [id-token topic-id db image-db]
  (err->>
   {:id-token id-token
    :db db
    :topic-id topic-id
    :image-db image-db}
   check-id-token
   check-topic-owner
   delete-topic
   delete-topic-image))
