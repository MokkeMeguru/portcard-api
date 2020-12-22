(ns portcard-api.usecase.check-account-name
  (:require [portcard-api.interface.database.users-repository :as users-repository]))

(defn user-already-exist? [user]
  (let [[status user] user]
    (not= :empty-user status)))

(defn check-account-name [uname db]
  (let [user (users-repository/get-user db :uname uname)]
    {:status 201
     :body {:exist (user-already-exist? user)}}))
