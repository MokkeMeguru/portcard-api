(ns portcard-api.interface.database.user-role-links-repository
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [orchestra.spec.test :as st]
            [portcard-api.domain.base :as base-model]
            [portcard-api.domain.user-roles :as user-roles-model]
            [portcard-api.interface.database.utils :as utils]))

;; define protocol
(defprotocol UserRoleLinks
  (get-links [db])
  (get-links-by-role-id [db role-id])
  (create-links [db link])
  (delete-links [db role-uid]))

(defn user-role-links-repository? [inst]
  (satisfies? UserRoleLinks inst))


;; define model parser


(defn ->user-role-link [user-role-link-db]
  (let [{:keys [uid link_category_name link_blob created_at updated_at]} user-role-link-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:link_uid uid
      :link_category_name link_category_name
      :link_blob link_blob
      :created_at created_at
      :updated_at updated_at})))

;; define spec
(s/def ::user-role-links-repository user-role-links-repository?)
(s/fdef get-links
  :args (s/cat :db ::user-role-links-repository)
  :ret ::user-roles-model/user-role-links)

(s/fdef get-links-by-role-id
  :args (s/cat :db ::user-role-links-repository :role-id ::user-roles-model/link_uid)
  :ret ::user-roles-model/user-role-links)

(s/fdef create-links
  :args (s/cat :db ::user-role-links-repository :link ::user-roles-model/creation-user-role-link)
  :ret ::user-roles-model/user-role-link)

(s/fdef delete-links
  :args (s/cat :db ::user-role-links-repository :role-uid ::user-roles-model/user_role_uid)
  :ret ::base-model/boolean)

;; define implementation
(extend-protocol UserRoleLinks
  portcard_api.infrastructure.sql.sql.Boundary
  (get-links [{:keys [spec]}]
    {:post [(s/valid? ::user-roles-model/user-role-links %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn ["SELECT * FROM user_role_links"] {:builder-fn rs/as-unqualified-lower-maps})
           (mapv #(into {} %))
           (map ->user-role-link))))

  (get-links-by-role-id [{:keys [spec]} role-uid]
    {:pre [(s/valid? ::user-roles-model/link_uid role-uid)]
     :post [(s/valid? ::user-roles-model/user-role-links %)]}
    (println role-uid)
    (->> (utils/find-by-m spec :user_role_links {:user_role_uid role-uid})
         (mapv #(into {} %))
         (map ->user-role-link)))

  (create-links [{:keys [spec]} link]
    {:pre [(s/valid? ::user-roles-model/creation-user-role-link link)]
     :post [(s/valid? ::user-roles-model/user-role-link %)]}
    (->> (utils/insert! spec :user_role_links link)
         (into {})
         ->user-role-link))

  (delete-links [{:keys [spec]} role-uid]
    {:pre [(s/valid? ::user-roles-model/user_role_uid role-uid)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/delete! spec :user_role_links {:user_role_uid role-uid})
         zero?
         not)))

;; ;; test
;; (st/instrument)
;; (defonce inst (portcard-api.infrastructure.sql.sql/->Boundary
;;                {:datasource (hikari-cp.core/make-datasource
;;                              {:jdbc-url (environ.core/env :database-url)})}))
;; (def sample-uuid2 (java.util.UUID/fromString "2667ca70-d17b-4f2a-818c-b861b14bcc35"))
;; (def sample-uuid (java.util.UUID/fromString "f4c1ab06-b103-4768-a64d-7c49592e43ec"))
;; (def sample-uid "b3mXXLoTA1QeLb1UoiknB3eerwn1")

;; (def sample-role-link
;;   {:user_role_uid sample-uuid
;;    :uid sample-uuid2
;;    :link_category_name "Github"
;;    :link_blob "https://github.com/MokkeMeguru"})

;; (create-links inst sample-role-link)
;; (get-links inst)

;; (get-links-by-role-id inst sample-uuid)
;; ;; (delete-links inst sample-uuid)
;; (st/unstrument)
