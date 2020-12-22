(ns portcard-api.usecase.signup
  (:require
   [portcard-api.util :refer [err->>]]
   [portcard-api.domain.errors :as errors]
   [portcard-api.interface.database.users-repository :as users-repository]
   [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
   [clojure.spec.alpha :as s]))

(defn user-name-is-used? [user]
  (if (= :empty-user (first user))
    [user nil]
    [nil errors/duplicate-user-name]))

(defn user-already-exist? [user]
  (if (= :empty-user (first user))
    [user nil]
    [nil errors/duplicate-account]))

(defn create-account [uname user-id db]
  (let [user {:uname uname
              :uid user-id
              :display_name uname}]
    (try
      (let [result (users-repository/create-user db user)]
        (if (and (not (= ::s/invalid result))
                 (= (:uid (second result)) user-id))
          {:status 201
           :body {:uname uname}}
          errors/user-creation-error))
      (catch Exception e
        (errors/unknown-error (.getMessage e))))))

(defn signup [uname id-token db]
  (let [{:keys [result user-id cause]} (safe-decode-token id-token)]
    (if (= :succcess result)
      (let [[_ name-err] (err->>
                          (users-repository/get-user db :uid user-id)
                          errors/sql-error
                          user-name-is-used?)
            [_ account-err] (err->>
                             (users-repository/get-user db :uname uname)
                             errors/sql-error
                             user-already-exist?)]
        (cond
          (not (nil? account-err)) account-err
          (not (nil? name-err)) name-err
          :default
          (create-account uname user-id db)))
      cause)))
