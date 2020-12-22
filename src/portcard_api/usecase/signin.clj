(ns portcard-api.usecase.signin
  (:require [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.domain.errors :as errors]
            [portcard-api.util :refer [err->> border-error]]
            [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [taoensso.timbre :as timbre]))

(defn user-not-found [user]
  (if (empty? user)
    [nil errors/user-not-found]
    [user nil]))

(defn signin [id-token db]
  (let [{:keys [result user-id cause]} (safe-decode-token id-token)]
    (if (= :success result)
      (let [[user err] (err->>
                        {:function #(users-repository/get-user db :uid user-id)
                         :error-wrapper errors/database-error}
                        border-error
                        user-not-found)]
        (if (nil? err)
          {:status 201
           :body {:uname (:uname user)}}
          err))
      cause)))
