(ns portcard-api.usecase.get-user-topics
  (:require [portcard-api.domain.errors :as errors]
            [portcard-api.domain.user-roles :as user-roles-model]
            [portcard-api.interface.database.user-topics-repository :as user-topics-repository]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.util :refer [border-error err->>]]
            [portcard-api.util :as util]))

(defn user-exist? [{:keys [uname db] :as m}]
  (let [[user err] (err->> {:function #(users-repository/get-user db :uname uname)
                            :error-wrapper errors/database-error}
                           border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? user) [nil errors/user-not-found]
      :else
      [(assoc m :user-id (:uid user)) nil])))

(defn get-topics [{:keys [user-id db from take category order] :as m}]
  (let [category-idx (when-not (nil? category) (user-roles-model/role-category category))
        [topics err] (err->> {:function #(user-topics-repository/get-user-topics-chunk db user-id from take category-idx order)
                              :error-wrapper errors/database-error}
                             border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else
      [(assoc m :topics topics) nil])))

(defn get-user-topics [uname from take category order db]
  (err->>
   {:uname uname :from from :take take :category category :order order :db db}
   user-exist?
   get-topics))
