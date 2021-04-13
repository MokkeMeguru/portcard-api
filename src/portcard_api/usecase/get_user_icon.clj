(ns portcard-api.usecase.get-user-icon
  (:require [portcard-api.domain.errors :as errors]
            [portcard-api.interface.database.user-profiles-icons-repository :as user-profiles-icons-repository]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.image-db.icons-repository :as icons-repository]
            [portcard-api.util :refer [err->> border-error]]))

(defn user-exist? [user]
  (and (-> user empty? not)
       (-> user :is_deleted not)))

(defn check-user-exist [{:keys [db uname] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uname uname)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      (not (user-exist? user)) [nil errors/user-not-found]
      :else [(assoc m :user user) nil])))

(defn get-user-icon-blob [{:keys [db user] :as m}]
  (let [uid (:uid user)
        [icon err] (err->>
                    {:function #(user-profiles-icons-repository/get-icon db uid)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else [(assoc m :icon-blob (:icon_blob icon)) nil])))

(defn get-user-icon-image [{:keys [icon-blob image-db] :as m}]
  (let [[icon err] (err->>
                    {:function #(icons-repository/get-icon image-db icon-blob)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? icon) [nil errors/icon-not-found]
      :else [(assoc m :icon icon) nil])))

(defn get-user-icon [uname icon-blob db image-db]
  (err->>
   {:uname uname :db db :image-db image-db :icon-blob icon-blob}
   check-user-exist
   get-user-icon-blob
   get-user-icon-image))
