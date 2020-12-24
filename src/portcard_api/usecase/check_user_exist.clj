(ns portcard-api.usecase.check-user-exist
  (:require [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.domain.errors :as errors]
            [portcard-api.util :refer [err->> border-error remove-empty]]))

(defn user-already-exist? [user]
  (not (empty? user)))

(defn check-user-exist [{:keys [db user-id] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uid user-id)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      (not (user-already-exist? user)) [nil errors/user-not-found]
      :else [(assoc m :user user) nil])))
