(ns portcard-api.usecase.check-account-name
  (:require [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.util :refer [err->> border-error]]
            [portcard-api.domain.errors :as errors]))

(defn user-already-exist? [user]
  (not (empty? user)))

(defn check-account-name [uname db]
  (let [[user-exist err] (err->>
                          {:function #(-> (users-repository/get-user db :uname uname)
                                          user-already-exist?)
                           :error-wrapper errors/database-error}
                          border-error)]
    [user-exist err]))
