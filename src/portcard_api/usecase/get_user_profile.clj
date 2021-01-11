(ns portcard-api.usecase.get-user-profile
  (:require [clojure.spec.alpha :as s]
            [portcard-api.usecase.check-user-exist :as check-user-exist-usecase]
            [portcard-api.usecase.check-id-token :as check-id-token]
            [portcard-api.util :refer [err->> border-error]]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.database.user-profiles-icons-repository :as user-profiles-icons-repository]
            [portcard-api.interface.database.user-profiles-contacts-repository :as user-profiles-contacts-repository]
            [portcard-api.domain.errors :as errors]
            [portcard-api.interface.database.user-roles-repository :as user-roles-repository]
            [portcard-api.interface.database.user-role-links-repository :as user-role-links-repository]
            [portcard-api.domain.user-roles :as user-roles-model]))

(defn get-user-role-links [{:keys [db uid] :as role}]
  (let [[user-role-links err] (err->> {:function #(user-role-links-repository/get-links-by-role-id db uid)
                                       :error-wrapper errors/database-error}
                                      border-error)]
    ;; TODO: fine
    (if (nil? err)
      (assoc role :role-links user-role-links)
      nil)))

(defn get-user-roles [{:keys [db user-id] :as m}]
  (let [[user-roles err] (err->> {:function #(user-roles-repository/get-user-role db user-id)
                                  :error-wrapper errors/database-error}
                                 border-error)]
    (println user-roles)
    (if (nil? err)
      [(assoc m :roles (mapv  #(get-user-role-links (merge {:db db} %)) user-roles)) nil]
      [nil err])))

(defn get-user-contact [{:keys [db user-id] :as m}]
  (println "user-id " user-id)
  (let [[contact err] (err->> {:function #(user-profiles-contacts-repository/get-contact db user-id)
                               :error-wrapper errors/database-error}
                              border-error)]
    (if (nil? err)
      [(assoc m :contact contact) nil]
      [nil err])))

(defn get-user-icon-blob [{:keys [db user-id] :as m}]
  (let [[icon err] (err->> {:function #(user-profiles-icons-repository/get-icon db user-id)
                            :error-wrapper errors/database-error}
                           border-error)]
    (if (nil? err)
      [(assoc m :icon_blob (:icon_blob icon)) nil]
      [nil err])))

(defn get-user-display-name [{:keys [db user] :as m}]
  [(assoc m :display-name (:display_name user)) nil])

(defn user-exist? [{:keys [db uname] :as m}]
  (let [[user err] (err->> {:function #(users-repository/get-user db :uname uname)
                            :error-wrapper errors/database-error}
                           border-error)]
    (println user)
    (cond
      (not (nil? err)) [nil err]
      (empty? user) [nil errors/user-not-found]
      :else
      [(-> m (assoc :user user)
           (assoc :user-id (:uid user))) nil])))

(defn get-user-profile [uname db]
  (err->>
   {:uname uname :db db}
   user-exist?
   get-user-display-name
   get-user-icon-blob
   get-user-contact
   get-user-roles))
