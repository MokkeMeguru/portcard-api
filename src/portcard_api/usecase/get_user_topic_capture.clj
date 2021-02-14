(ns portcard-api.usecase.get-user-topic-capture
  (:require [portcard-api.interface.database.user-topics-repository :as user-topics-repository]
            [portcard-api.util :refer [err->> border-error]]
            [portcard-api.domain.errors :as errors]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.image-db.topic-captures-repository :as topic-captures-repository]))

(defn user-exist? [user]
  (and (-> user empty? not)
       (-> user :is_deleted not)))

(defn check-user-exist [{:keys [db uname] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uname uname)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      (not (user-exist? user)) [nil errors/user-not-found]
      :else [(assoc m :user user) nil])))

(defn check-topic-exist [{:keys [db topic-id] :as m}]
  (let [[topic err] (err->>
                     {:function #(user-topics-repository/get-user-topic-by-id db topic-id)
                      :error-wrapper errors/database-error}
                     border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? topic) [nil errors/topic-not-found]
      :else [(assoc m :topic topic) nil])))

(defn get-user-topic-capture-image [{:keys [topic image-db] :as m}]
  (let [image_blob (:image_blob topic)
        [image err] (err->>
                     {:function #(topic-captures-repository/get-capture image-db image_blob)
                      :error-wrapper errors/database-error}
                     border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? image) [nil errors/topic-capture-not-found]
      :else [(assoc m :image image) nil])))

(defn get-user-topic-capture [user-id topic-id image-blob db image-db]
  (err->>
   {:uname user-id
    :topic-id topic-id
    :image-blob image-blob
    :db db
    :image-db image-db}
   check-user-exist
   check-topic-exist
   get-user-topic-capture-image))
