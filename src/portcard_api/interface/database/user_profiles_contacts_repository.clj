(ns portcard-api.interface.database.user-profiles-contacts-repository
  (:require [portcard-api.infrastructure.sql.sql]
            [portcard-api.interface.database.utils :as utils]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.spec.alpha :as s]
            [portcard-api.domain.users :as user-model]
            [portcard-api.domain.base :as base-model]
            [orchestra.spec.test :as st]))

;; define protocol
(defprotocol UserProfilesContacts
  (get-contacts [db])
  (get-contact [db uid])
  (create-contact [db contact])
  (update-contact [db m idm])
  (delete-contact [db uid]))

(defn user-profiles-contacts-repository? [inst]
  (satisfies? UserProfilesContacts inst))

;; define model parser
(defn ->user-profiles-contact [user-profiles-contact-db]
  (let [{:keys [email twitter facebook created_at updated_at]} user-profiles-contact-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:email email
      :twitter twitter
      :facebook facebook
      :created_at created_at
      :updated_at updated_at})))

;; define spec
(s/def ::user-profiles-contacts-repository user-profiles-contacts-repository?)
(s/fdef get-contacts
  :args (s/cat :db ::user-profiles-contacts-repository)
  :ret ::user-model/user-profiles-contacts)

(s/fdef get-contact
  :args (s/cat :db ::user-profiles-contacts-repository :uid ::user-model/uid)
  :ret (s/or :contact-exist ::user-model/user-profiles-contact :contact-not-exist ::base-model/empty))

(s/fdef create-contact
  :args (s/cat :db ::user-profiles-contacts-repository :contact ::user-model/creation-user-profiles-contact)
  :ret ::user-model/user-profiles-contact)

(s/fdef update-contact
  :args (s/cat :db ::user-profiles-contacts-repository :m ::base-model/map :idm ::base-model/map)
  :ret ::base-model/boolean)

(s/fdef delete-contact
  :args (s/cat :db ::user-profiles-contacts-repository :uid ::user-model/uid)
  :ret ::base-model/boolean)

;; define implementation
(extend-protocol UserProfilesContacts
  portcard_api.infrastructure.sql.sql.Boundary
  (get-contacts [{:keys [spec]}]
    {:post [(s/valid? ::user-model/user-profiles-contacts %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn ["SELECT * FROM user_profiles_contacts"] {:builder-fn rs/as-unqualified-lower-maps})
           (mapv #(into {} %))
           (map ->user-profiles-contact))))

  (get-contact [{:keys [spec]} uid]
    {:pre [(s/valid? ::user-model/uid uid)]
     :post [(or (s/valid? ::user-model/user-profiles-contact %)
                (s/valid? ::base-model/empty %))]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (utils/get-by-id spec :user_profiles_contacts :user_uid uid)
           (into {})
           ->user-profiles-contact)))

  (update-contact [{:keys [spec]} m idm]
    {:pre [(s/valid? ::base-model/map m)
           (s/valid? ::base-model/map idm)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/update! spec :user_profiles_contacts m idm)
         zero?
         not))

  (create-contact [{:keys [spec]} contact]
    {:pre [(s/valid? ::user-model/creation-user-profiles-contact contact)]
     :post [(s/valid? ::user-model/user-profiles-contact %)]}
    (->> (utils/insert! spec :user_profiles_contacts contact)
         ->user-profiles-contact))

  (delete-contact [{:keys [spec]} uid]
    {:pre [(s/valid? ::user-model/uid uid)]
     :post [(s/valid? ::base-model/boolean  %)]}
    (->>  (utils/delete! spec :user_profiles_contacts {:user_uid uid})
          zero?
          not)))

;; test
;; (st/instrument)
;; (defonce inst (portcard-api.infrastructure.sql.sql/->Boundary
;;                {:datasource (hikari-cp.core/make-datasource
;;                              {:jdbc-url (environ.core/env :database-url)})}))
;; (satisfies? UserProfilesContacts inst)
;; (def sample-uid "b3mXXLoTA1QeLb1UoiknB3eerwn1")
;; (when (empty? (portcard-api.interface.database.users-repository/get-user inst :uid sample-uid))
;;   (portcard-api.interface.database.users-repository/create-user
;;    inst {:uname "Meguru" :uid "b3mXXLoTA1QeLb1UoiknB3eerwn1" :display_name "MokkeMeguru"}))
;; (def sample-contact {:user_uid sample-uid :email "mokke.mokke@gmail.com" :twitter "@MeguruMokke"})
;; (create-contact inst sample-contact)
;; (get-contacts inst)
;; (get-contact inst sample-uid)
;; (update-contact inst {:email "meguru.mokke@gmail.com" :twitter "@MeguruMokke"} {:user_uid sample-uid})
;; (delete-contact inst sample-uid)
;; (st/unstrument)

;; other-comment
;; (s/explain ::user-model/user-profiles-contact (first (get-contacts inst)))
;; (get-contacts inst)

;; (get-contact inst "xxx-xxx")
;; (create-contact inst
;;                 {:user_uid sample-uid
;;                  :email "meguru.mokke@gmail.com"
;;                  :twitter "@MeguruMokke"})

;; (s/valid? ::user-model/twitter nil)
;; (s/def ::sample (s/keys :opt-un [::user-model/twitter]))
;; (s/valid? ::sample  {:twitter nil})
;; (update-contact inst {:email "meguru.mokke@gmail.com"} {:user_uid sample-uid})
