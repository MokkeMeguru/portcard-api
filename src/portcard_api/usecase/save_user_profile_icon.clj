(ns portcard-api.usecase.save-user-profile-icon
  (:require [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.image-db.icons-repository :as icons-repository]
            [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
            [portcard-api.domain.errors :as errors]
            [portcard-api.domain.images :as images-domain]
            [portcard-api.interface.image-processor.core :as image-processor]
            [portcard-api.util :refer [err->> border-error rand-str]]
            [portcard-api.util :as util]
            [portcard-api.interface.database.user-profiles-icons-repository :as user-profiles-icons-repository]))

(defn user-exist? [user]
  (and (-> user empty? not)
       (-> user :is_deleted not)))

(defn check-user-exist [{:keys [db user-id] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uid user-id)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      (not (user-exist? user)) [nil errors/user-not-found]
      :else [(assoc m :user user) nil])))

(defn ->icon-image [{:keys [icon-image] :as m}]
  (try [(assoc m :icon-image (image-processor/->icon icon-image images-domain/icon-max-size)) nil]
       (catch Exception e
         [nil (errors/unknown-error (.getMessage e))])))

(defn save-icon-image-into-image-db [{:keys [icon-image image-db] :as m}]
  (let [icon-blob (str (rand-str images-domain/icon-blob-length) ".png")
        [status err] (err->> {:function #(icons-repository/insert-icon image-db icon-image icon-blob)
                              :error-wrapper errors/database-error}
                             border-error)]
    (cond
      (not (nil? err)) [nil err]
      (not status) [nil errors/icon-save-failed]
      :else
      [(assoc m :icon-blob icon-blob) nil])))

(defn save-icon-image-into-sql-db [{:keys [icon-blob db user] :as m}]
  (err->> {:function #(user-profiles-icons-repository/create-icon db {:user_uid (:uid user) :icon_blob icon-blob})
           :error-wrapper errors/database-error}
          border-error))

(defn delete-icon [{:keys [user db image-db] :as m}]
  (let [[icon err] (err->> {:function #(user-profiles-icons-repository/get-icon db (:uid user))
                            :error-wrapper errors/database-error}
                           border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? icon) [m nil]
      :else (let [[_ err] (err->> {:function #(do
                                                (user-profiles-icons-repository/delete-icon db (:uid user))
                                                (icons-repository/delete-icon image-db  (:icon_blob icon)))
                                   :error-wrapper errors/database-error}
                                  border-error)]
              (if (nil? err)
                [m nil]
                [nil err])))))

(defn check-token [m]
  (let [{:keys [result user-id cause]} (safe-decode-token (:id-token m))]
    (if (= :success result)
      [(assoc m :user-id user-id) nil]
      [nil cause])))

(defn save-user-profile-icon [{:keys [icon-image image-db db id-token]}]
  (err->>
   {:id-token id-token
    :image-db image-db
    :icon-image icon-image
    :db db}
   check-token
   check-user-exist
   ->icon-image
   delete-icon
   save-icon-image-into-image-db
   save-icon-image-into-sql-db))
