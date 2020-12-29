(ns portcard-api.usecase.upsert-user-profile
  (:require [portcard-api.interface.firebase.auth :refer [safe-decode-token]]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.database.user-profiles-contacts-repository :as user-profiles-contacts-repository]
            [portcard-api.interface.database.user-roles-repository :as user-roles-repository]
            [portcard-api.interface.database.user-role-links-repository :as user-role-links-repository]
            [portcard-api.usecase.check-user-exist :as check-user-exist-usecase]
            [portcard-api.util :refer [err->> border-error remove-empty]]
            [portcard-api.domain.errors :as errors]
            [portcard-api.domain.user-roles :as user-roles-model]))

(defn user-not-found [user]
  (empty? user))

(defn upsert-display-name [user-id display-name db]
  (if (nil? display-name)
    [nil nil]
    (err->> {:function #(users-repository/update-user db {:display_name display-name} {:uid user-id})
             :error-wrapper errors/database-error}
            border-error)))

(defn upsert-contact [user-id contact db]
  (if (nil? contact)
    [nil nil]
    (let [{:keys [email twitter facebook]} contact
          contact-model (remove-empty
                         {:email email
                          :twitter twitter
                          :facebook facebook})
          [contact err] (err->> {:function #(user-profiles-contacts-repository/get-contact db user-id)
                                 :error-wrapper errors/database-error}
                                border-error)]
      (cond
        (-> err not nil?) [nil err]
        (-> contact empty?)
        (err->> {:function #(user-profiles-contacts-repository/create-contact db (assoc contact-model :user_uid user-id))
                 :error-wrapper errors/database-error}
                border-error)
        :else
        (err->> {:function #(user-profiles-contacts-repository/update-contact db contact-model {:user_uid user-id})
                 :error-wrapper errors/database-error}
                border-error)))))

(defn insert-role-link [role-uuid role-link db]
  (let [role-link-uuid (java.util.UUID/randomUUID)
        {:keys [link-category-name link-url]} role-link
        role-link-model {:user_role_uid role-uuid
                         :uid role-link-uuid
                         :link_category_name link-category-name ;; (user-roles-model/link-category link-category)
                         :link_blob link-url}]
    (err->> {:function #(user-role-links-repository/create-links db role-link-model)
             :error-wrapper errors/database-error}
            border-error)))

(defn insert-role-links [role-uuid role-links db]
  (loop [_role-links role-links
         status []
         err nil]
    (if (zero? (count _role-links))
      [status err]
      (let [[new-status new-err] (insert-role-link role-uuid (first _role-links) db)]
        (recur (rest _role-links) (cons new-status status) (if (nil? new-err) err new-err))))))

(defn insert-role [user-id role db]
  (let [{:keys [role-category primary-rank role-links]} role
        role-uuid (java.util.UUID/randomUUID)
        user-role-model {:uid role-uuid
                         :user_uid user-id
                         :category (user-roles-model/role-category role-category)
                         :primary_rank primary-rank}
        [insert-role-status err] (err->> {:function #(user-roles-repository/create-user-role db user-role-model)
                                          :error-wrapper errors/database-error}
                                         border-error)]
    ;; TODO: fine
    (if (nil? err)
      (insert-role-links role-uuid role-links db)
      [nil err])))

(defn delete-all-roles [user-id db]
  (err->> {:function #(user-roles-repository/delete-user-role db user-id)
           :error-wrapper errors/database-error}
          border-error))

(defn upsert-roles
  "1. delete all roles and role-links (using sql cascading TODO: how to implement in memory database?)
   2. insert each roles
     2-1. insert role of roles
     2-2. insert role-links
       2-2-1. insert role-link of role-links"
  [user-id roles db]

  (if (nil? roles)
    [nil nil]
    (let [[_ err] (delete-all-roles user-id db)]
      (if-not (nil? err)
        [nil err]
        ;; TODO: implement insert-multi!
        (loop [_roles roles
               status []
               err nil]
          (if (zero? (count _roles))
            [status err]
            (let [[new-status new-err] (insert-role user-id (first _roles) db)]
              (recur (rest _roles) (cons new-status status) (if (nil? new-err) err new-err)))))))))

(defn check-token [m]
  (let [{:keys [result user-id cause]} (safe-decode-token (:id-token m))]
    (if (= :success result)
      [(assoc m :user-id user-id) nil]
      [nil cause])))

(defn upsert-all-user-profile [{:keys [user-id display-name contact roles db] :as m}]
  (let [[_ upsert-display-name-err] (upsert-display-name user-id display-name db)
        [_ upsert-contact-err]  (upsert-contact user-id contact db)
        [_ upsert-roles-err] (upsert-roles user-id roles db)]
    (cond
      (-> upsert-display-name-err nil? not) [nil upsert-display-name-err]
      (-> upsert-contact-err nil? not) [nil upsert-contact-err]
      (-> upsert-roles-err nil? not) [nil upsert-roles-err]
      :else [m nil])))

(defn upsert-user-profile [id-token display-name contact roles db]
  (err->>
   {:id-token id-token
    :display-name display-name
    :contact contact
    :roles roles
    :db db}
   check-token
   check-user-exist-usecase/check-user-exist
   upsert-all-user-profile))
