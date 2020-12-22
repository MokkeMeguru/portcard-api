(ns portcard-api.interface.database.user-profiles-icons-repository
  (:require [portcard-api.interface.database.utils :as utils]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [portcard-api.domain.users :as user-model]
            [next.jdbc.result-set :as rs]
            [portcard-api.domain.base :as base-model]
            [orchestra.spec.test :as st]))

;; define protocol
(defprotocol UserProfilesIcons
  (get-icons [db])
  (get-icon [db uid])
  (update-icon [db m idm])
  (create-icon [db icon])
  (delete-icon [db uid]))

(defn user-profiles-icons-repository? [inst]
  (satisfies? UserProfilesIcons inst))

;; define model parser
(defn ->user-profiles-icon [user-profiles-icon-db]
  (let [{:keys [icon_blob created_at updated_at]} user-profiles-icon-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:icon_blob icon_blob
      :created_at created_at
      :updated_at updated_at})))

;; define spec
(s/def ::user-profiles-icons-repository user-profiles-icons-repository?)
(s/fdef get-icons
  :args (s/cat :db ::user-profiles-icons-repository)
  :ret ::user-model/user-profiles-icons)

(s/fdef get-icon
  :args (s/cat :db ::user-profiles-icons-repository :uid ::user-model/uid)
  :ret (s/or :icon-exist ::user-model/user-profiles-icon :icon-not-exist ::base-model/empty))

(s/fdef update-icon
  :args (s/cat :db ::user-profiles-icons-repository :m ::base-model/map :idm ::base-model/map)
  :ret  ::base-model/boolean)

(s/fdef create-icon
  :args (s/cat :db ::user-profiles-icons-repository :icon ::user-model/creation-user-profiles-icon)
  :ret ::user-model/user-profiles-icon)

(s/fdef delete-icon
  :args (s/cat :db ::user-profiles-icons-repository :uid ::user-model/uid)
  :ret ::base-model/boolean)

(extend-protocol UserProfilesIcons
  portcard_api.infrastructure.sql.sql.Boundary
  (get-icons [{:keys [spec]}]
    {:post [(s/valid? ::user-model/user-profiles-contacts %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn ["SELECT * FROM user_profiles_icons"] {:builder-fn rs/as-unqualified-lower-maps})
           (mapv #(into {} %))
           (map ->user-profiles-icon))))

  (get-icon [{:keys [spec]} uid]
    {:pre [(s/valid? ::user-model/uid uid)]
     :post [(or (s/valid? ::user-model/user-profiles-icon %) (s/valid? ::base-model/empty %))]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (utils/get-by-id spec :user_profiles_icons :user_uid uid)
           (into {})
           ->user-profiles-icon)))

  (create-icon [{:keys [spec]} icon]
    {:pre [(s/valid? ::user-model/creation-user-profiles-icon icon)]
     :post [(or (s/valid? ::user-model/user-profiles-icon %)
                (s/valid? ::base-model/empty %))]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (utils/insert! spec :user_profiles_icons icon)
           ->user-profiles-icon)))

  (update-icon [{:keys [spec]} m idm]
    {:pre [(s/valid? ::base-model/map m)
           (s/valid? ::base-model/map idm)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/update! spec :user_profiles_icons m idm)
         zero?
         not))

  (delete-icon [{:keys [spec]} uid]
    {:pre [(s/valid? ::user-model/uid uid)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->>  (utils/delete! spec :user_profiles_icons {:user_uid uid})
          zero?
          not)))

;; test
;; (st/instrument)
;; (defonce inst (portcard-api.infrastructure.sql.sql/->Boundary
;;                {:datasource (hikari-cp.core/make-datasource
;;                              {:jdbc-url (environ.core/env :database-url)})}))
;; (satisfies? UserProfilesIcons inst)
;; (def sample-uid "b3mXXLoTA1QeLb1UoiknB3eerwn1")
;; (when (empty? (portcard-api.interface.database.users-repository/get-user inst :uid sample-uid))
;;   (portcard-api.interface.database.users-repository/create-user
;;    inst {:uname "Meguru" :uid "b3mXXLoTA1QeLb1UoiknB3eerwn1" :display_name "MokkeMeguru"}))
;; (def sample-icon {:user_uid sample-uid :icon_blob "sample.png"})
;; (get-icons inst)
;; (get-icon inst sample-uid)
;; (create-icon inst sample-icon)
;; (update-icon inst {:icon_blob "sample2.png"} {:user_uid sample-uid})
;; (get-icons inst)
;; (get-icon inst sample-uid)
;; (delete-icon inst sample-uid)
;; (st/unstrument)
