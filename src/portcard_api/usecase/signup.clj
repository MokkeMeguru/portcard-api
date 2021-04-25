(ns portcard-api.usecase.signup
  (:require [clojure.spec.alpha :as s]
            [portcard-api.domain.errors :as errors]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
            [portcard-api.util :refer [border-error err->>]]))

(defn decode-id-token [{:keys [id-token] :as m}]
  (let [{:keys [result user-id cause]} (safe-decode-token id-token)]
    (if (= :success result)
      [(assoc m :user-id user-id) nil]
      [nil cause])))

(defn check-user-name-is-used [{:keys [uname db] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uname uname)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (some? err) [nil err]
      (some? user)  [nil errors/duplicate-user-name]
      :else [(assoc m :user user) nil])))

(defn check-user-already-exist [{:keys [user-id db] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uid user-id)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (some? err) [nil err]
      (some? user) [nil errors/duplicate-account]
      :else [m nil])))

(defn create-account [{:keys [uname user-id db] :as m}]
  (let [user {:uname uname
              :uid user-id
              :display_name uname}
        [result err] (err->>
                      {:function #(users-repository/create-user db user)
                       :error-wrapper errors/database-error}
                      border-error)]
    (cond
      (some? err) [nil err]
      (not= user-id (:uid user)) [nil errors/user-creation-error]
      :else [(assoc m :user user) nil])))

(defn signup [uname id-token db]
  (err->>
   {:uname uname :id-token id-token :db db}
   decode-id-token
   check-user-already-exist
   check-user-name-is-used
   create-account))
