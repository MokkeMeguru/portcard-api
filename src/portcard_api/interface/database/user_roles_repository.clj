(ns portcard-api.interface.database.user-roles-repository
  (:require [portcard-api.interface.database.utils :as utils]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.spec.alpha :as s]
            [portcard-api.domain.user-roles :as user-roles-model]
            [portcard-api.domain.users :as user-model]
            [portcard-api.domain.base :as base-model]
            [orchestra.spec.test :as st]))

;; define protocol
(defprotocol UserRoles
  (get-user-roles [db])
  (get-user-role [db uid])
  (get-user-role-with-rank [db uid primary-rank])
  (create-user-role [db user-role])
  (update-user-role [db m idm])
  (delete-user-role [db uid])
  (delete-user-role-over-rank [db uid primary-rank]))

(defn user-roles-repository? [inst]
  (satisfies? UserRoles inst))

;; define model parser
(defn ->user-role [user-role-db]
  (let [{:keys [uid category primary_rank created_at updated_at]} user-role-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:uid uid
      :category category
      :primary_rank primary_rank
      :created_at created_at
      :updated_at updated_at})))

;; define spec
(s/def ::user-roles-repository user-roles-repository?)
(s/fdef get-user-roles
  :args (s/cat :db ::user-roles-repository)
  :ret ::user-roles-model/user-roles)

(s/fdef get-user-role
  :args (s/cat :db ::user-roles-repository :uid ::user-model/uid)
  :ret ::user-roles-model/user-roles)

(s/fdef get-user-role-with-rank
  :args (s/cat :db ::user-roles-repository :uid ::user-model/uid :primary-rank ::user-roles-model/primary_rank)
  :ret (s/or :role-exist ::user-roles-model/user-role :role-not-exist ::base-model/empty))

(s/fdef create-user-role
  :args (s/cat :db ::user-roles-repository ::user-role ::user-roles-model/creation-user-role)
  :ret ::user-roles-model/user-role)

(s/fdef update-user-role
  :args (s/cat :db ::user-roles-repository :m ::base-model/map :idm ::base-model/map)
  :ret ::base-model/boolean)

(s/fdef delete-user-role
  :args (s/cat :db ::user-roles-repository :uid ::user-model/uid)
  :ret ::base-model/boolean)

(s/fdef delete-user-role-over-rank
  :args (s/cat :db ::user-roles-repository :uid ::user-model/uid :primary-rank ::user-roles-model/primary_rank)
  :ret ::base-model/boolean)

(extend-protocol UserRoles
  portcard_api.infrastructure.sql.sql.Boundary
  (get-user-roles [{:keys [spec]}]
    {:post [(s/valid? ::user-roles-model/user-roles %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn ["SELECT * FROM user_roles"] {:builder-fn rs/as-unqualified-lower-maps})
           (mapv #(into {} %))
           (map ->user-role))))

  (get-user-role [{:keys [spec]} uid]
    {:pre [(s/valid? ::user-model/uid uid)]
     :post [(s/valid? ::user-roles-model/user-roles %)]}
    (->> (utils/find-by-m spec :user_roles {:user_uid uid})
         (mapv #(into {} %))
         (map ->user-role)))

  (get-user-role-with-rank [{:keys [spec]} uid primary-rank]
    {:pre [(s/valid? ::user-model/uid uid) (s/valid? ::user-roles-model/primary_rank primary-rank)]
     :post [(or (s/valid? ::user-roles-model/user-role) (s/valid? ::base-model/empty))]}
    (->> (utils/find-by-m spec :user_roles {:user_uid uid :primary_rank primary-rank})
         first
         (into {})
         ->user-role))

  (create-user-role [{:keys [spec]} user-role]
    {:pre [(s/valid? ::user-roles-model/creation-user-role user-role)]
     :post [(s/valid? ::user-roles-model/user-role %)]}
    (->> (utils/insert! spec :user_roles user-role)
         ->user-role))

  (update-user-role [{:keys [spec]} m idm]
    {:pre [(s/valid? ::base-model/map m)
           (s/valid? ::base-model/idm idm)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/update! spec :user_roles m idm)
         zero?
         not))

  (delete-user-role [{:keys [spec]} uid]
    {:pre [(s/valid? ::user-model/uid uid)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/delete! spec :user_roles {:user_uid uid})
         zero?
         not))

  (delete-user-role-over-rank [{:keys [spec]} uid primary-rank]
    {:pre [(s/valid? ::user-model/uid uid)
           (s/valid? ::user-roles-model/primary_rank primary-rank)]
     :post [(s/valid? ::base-model/boolean %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute-one! conn ["DELETE FROM user_roles WHERE user_uid = ? AND primary_rank >= ?" uid primary-rank])
           :next.jdbc/update-count
           zero?
           not))))

;; ;; test
;; (st/instrument)
;; (defonce inst (portcard-api.infrastructure.sql.sql/->Boundary
;;                {:datasource (hikari-cp.core/make-datasource
;;                              {:jdbc-url (environ.core/env :database-url)})}))

;; (def sample-uuid (java.util.UUID/fromString "2667ca70-d17b-4f2a-818c-b861b14bcc34"))
;; (def sample-uuid2 (java.util.UUID/fromString "2667ca70-d17b-4f2a-818c-b861b14bcc35"))
;; (def sample-uid "b3mXXLoTA1QeLb1UoiknB3eerwn1")

;; (user-roles-repository? inst)
;; (delete-user-role inst sample-uid)
;; (delete-user-role-over-rank inst sample-uid 0)

;; (def sample-role
;;   {:uid sample-uuid
;;    :user_uid sample-uid
;;    :category 0
;;    :primary_rank 0})

;; (def sample-role2
;;   {:uid sample-uuid2
;;    :user_uid sample-uid
;;    :category 1
;;    :primary_rank 1})

;; (delete-user-role-over-rank inst sample-uid 0)
;; (create-user-role inst sample-role)
;; (false? (delete-user-role-over-rank inst sample-uid 1))
;; (delete-user-role-over-rank inst sample-uid 0)
;; (create-user-role inst sample-role)
;; (create-user-role inst sample-role2)
;; (get-user-roles inst)
;; (get-user-role inst sample-uid)
;; (delete-user-role inst sample-uid)
;; (get-user-roles inst)
;; (st/unstrument)


;; TODO: clojure's problem
;; saftisfies? sometimes wrong attribute in repl
;; (defprotocol Foo
;;   (foo [this])
;;   (foo-foo [this times])
;;   (foo-foo-foo [this times]))

;; (extend-protocol Foo
;;   java.lang.Number
;;   (foo [this]
;;     "foo by number")
;;   (foo-foo [this times]
;;     (repeat times "foo by number sometimes"))
;;   (foo-foo-foo [this times]
;;     "foo foo foo"))

;; (satisfies? Foo java.lang.Number)
;; => often returns false
