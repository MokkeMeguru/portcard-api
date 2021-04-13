(ns portcard-api.usecase.get-latest-user-topics
  (:require [portcard-api.domain.errors :as errors]
            [portcard-api.domain.user-roles :as user-roles-model]
            [portcard-api.interface.database.user-topics-repository :as user-topics-repository]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.util :refer [err->> border-error]]))

(defn user-exist? [{:keys [uname db] :as m}]
  (let [[user err] (err->> {:function #(users-repository/get-user db :uname uname)
                            :error-wrapper errors/database-error}
                           border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? user) [nil errors/user-not-found]
      :else
      [(assoc m :user-id (:uid user)) nil])))

(defn get-topics [{:keys [user-id db from take category] :as m}]
  (let [category-idx (when-not (nil? category) (user-roles-model/role-category category))
        [topics err] (err->> {:function #(user-topics-repository/get-user-topics-chunk db user-id nil take category-idx "desc")
                              :error-wrapper errors/database-error}
                             border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else
      [(assoc m :topics topics) nil])))

(defn get-latest-user-topics [uname take db]
  (err->>
   {:uname uname :from 0 :take take :category nil :db db}
   user-exist?
   get-topics))
