(ns portcard-api.usecase.check-account-name
  (:require [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.util :refer [err->> border-error]]
            [portcard-api.domain.errors :as errors]))

(defn check-user-exist [{:keys [db uname] :as m}]
  (let [[user err] (err->> {:function #(users-repository/get-user db :uname uname)
                            :error-wrapper errors/database-error}
                           border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else [(-> m (assoc :user user)) nil])))

(defn account-name-exist [{:keys [user] :as m}]
  [(assoc m :user-exist (not (empty? user))) nil])

(defn check-account-name [uname db]
  (err->>
   {:uname uname :db db}
   check-user-exist
   account-name-exist))
