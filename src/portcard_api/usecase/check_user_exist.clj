(ns portcard-api.usecase.check-user-exist
  (:require [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.domain.errors :as errors]
            [portcard-api.util :refer [err->> border-error remove-empty]]))

(defn user-already-exist? [user]
  (not (empty? user)))

(defn check-user-exist [{:keys [db user-id] :as m}]
  (println "userid is :" m)
  (let [[user-exist err] (err->>
                          {:function #(-> (users-repository/get-user db :uid user-id)
                                          user-already-exist?)
                           :error-wrapper errors/database-error}
                          border-error)]
    (cond
      (not (nil? err)) [nil err]
      (not user-exist) [nil errors/user-not-found]
      :else [m nil])))
