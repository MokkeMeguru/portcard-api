(ns portcard-api.usecase.signin
  (:require [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [portcard-api.domain.errors :as errors]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
            [portcard-api.util :refer [border-error err->>]]
            [taoensso.timbre :as timbre]))

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

(defn signin [id-token db]
  (err->>
   {:id-token id-token :db db}
   check-id-token
   check-user-exist))
