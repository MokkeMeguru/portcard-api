(ns portcard-api.interface.database.users-repository
  (:require [portcard-api.infrastructure.sql.sql]
            [next.jdbc :as jdbc]
            [portcard-api.interface.database.utils :as utils]
            [clojure.spec.alpha :as s]
            [portcard-api.domain.users :as user-model]
            [portcard-api.domain.base :as base-model]
            [clj-time.coerce :as tc]
            [next.jdbc.result-set :as rs]
            [orchestra.spec.test :as st]
            [integrant.core :as ig]))

;; define protocol
(defprotocol Users
  (get-users [db])
  (get-user [db k v])
  (search-user [db uname-idea])
  (update-user [db m idm])
  (create-user [db user])
  (delete-user [db idm])
  ;; TODO: (deactivate-user [db uid])
  )

(defn users-repository? [inst]
  (satisfies? Users inst))


;; define model parser


(defn ->user [user-db]
  (let [{:keys [uname uid created_at updated_at display_name is_deleted]} user-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:uname uname
      :uid uid
      :created_at created_at
      :updated_at updated_at
      :display_name display_name
      :is_deleted is_deleted})))

;; define spec
(s/def ::users-repository users-repository?)
(s/fdef get-users
  :args (s/cat :db ::users-repository)
  :ret ::user-model/users)

(s/fdef get-user
  :args (s/cat :db ::users-repository :k ::base-model/keyword :v ::base-model/string)
  :ret (s/or :user-exist ::user-model/user :user-not-exist ::base-model/empty))

(s/fdef update-user
  :args (s/cat :db ::users-repository :m ::base-model/map :idm ::base-model/map)
  :ret ::base-model/boolean)

(s/fdef delete-user
  :args (s/cat :db ::users-repository :idm ::base-model/map)
  :ret ::base-model/boolean)

(s/fdef create-user
  :args (s/cat :db ::users-repository :user ::user-model/creation-user)
  :ret ::user-model/user)

(s/fdef search-user
  :args (s/cat :db ::users-repository :uname-idea ::base-model/string)
  :ret ::user-model/users)

;; define implementation


(extend-protocol Users
  portcard_api.infrastructure.sql.sql.Boundary
  (get-users [{:keys [spec]}]
    {:post [(s/valid? ::user-model/users %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->>  (jdbc/execute! conn ["SELECT * FROM users"]
                           {:builder-fn rs/as-unqualified-lower-maps})
            (mapv #(into {} %))
            (map ->user))))

  (get-user [{:keys [spec]} k v]
    {:pre [(s/valid? ::base-model/keyword k)
           (s/valid? ::base-model/string v)]
     :post [(or (s/valid? ::user-model/user %)
                (s/valid? ::base-model/empty %))]}
    (->> (utils/get-by-id spec :users k v)
         (into {})
         ->user))

  (update-user [{:keys [spec]} m idm]
    {:pre [(s/valid? ::base-model/map m)
           (s/valid? ::base-model/map idm)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/update! spec :users m idm)
         zero?
         not))

  (create-user [{:keys [spec]} user]
    {:pre [(s/valid? ::user-model/creation-user user)]
     :post [(or (s/valid? ::user-model/user %)
                (s/valid? ::base-model/empty %))]}
    (->> (utils/insert! spec :users user)
         ->user))

  (delete-user [{:keys [spec]} idm]
    {:pre [(s/valid? ::base-model/map idm)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/delete! spec :users idm)
         zero?
         not))

  (search-user [{:keys [spec]} uname-idea]
    {:pre [(s/valid? ::base-model/string uname-idea)]
     :post [(s/valid? ::user-model/users %)]}
    (let [search-uname-idea (str "%" uname-idea "%")]
      (with-open [conn (jdbc/get-connection (:datasource spec))]
        (->> (jdbc/execute! conn ["SELECT * FROM users WHERE display_name LIKE ? or uname LIKE ? and is_deleted = false"
                                  search-uname-idea search-uname-idea]
                            {:builder-fn rs/as-unqualified-lower-maps})
             (map #(into {} %))
             (map ->user))))))


;; ;; test
;; (st/instrument)


;; (defonce inst
;;   (portcard-api.infrastructure.sql.sql/->Boundary
;;    {:datasource
;;     (portcard-api.infrastructure.sql.sql/wrap-logger
;;      (hikari-cp.core/make-datasource
;;       {:jdbc-url (environ.core/env :database-url)}))}))

;; (get-users inst)
;; (delete-user inst {:uname "meguru2"})

;; (satisfies? Users inst)
;; (def sample-user {:uname "sample" :uid "xxx-xxx" :display_name "sample"})
;; (create-user inst sample-user)
;; (get-users inst)
;; (get-user inst :uname "sample")
;; (update-user inst {:display_name "hello"} {:uid "xxx-xxx"})
;; (= "hello" (:display_name (get-user inst :uname "sample")))
;; (-> (search-user inst "hel") count pos?)
;; (delete-user inst {:uid "xxx-xxx"})
;; (empty? (get-user inst :uid "xxx-xxx"))
;; (st/unstrument)



;; other comment
;; (create-user inst {:uname "Meguru" :uid "b3mXXLoTA1QeLb1UoiknB3eerwn1" :display_name "MokkeMeguru"})

;; (get-users inst)
;; (get-user inst :uname "Meguru")
;; (update-user inst {:display_name "MokkeMeguru"} {:uname "Meguru"})
