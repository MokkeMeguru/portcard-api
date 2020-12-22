(ns portcard-api.usecase.signin
  (:require [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.domain.errors :as errors]
            [portcard-api.util :refer [err->>]]
            [portcard-api.interface.firebase.auth :refer [safe-decode-token]]))

(defn user-not-found [user]
  (let [[status user] user]
    (if (= :empty-user status)
      [nil errors/user-not-found]
      [user nil])))

(defn signin [id-token db]
  (let [{:keys [result user-id cause]} (safe-decode-token id-token)]
    (if (= :succcess result)
      (let [[user err] (err->>
                        (users-repository/get-user db :uid user-id)
                        errors/sql-error
                        user-not-found)]
        (if (nil? err)
          {:status 201
           :body {:uname (:uname user)}}
          err))
      cause)))
