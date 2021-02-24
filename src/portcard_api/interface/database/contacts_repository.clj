(ns portcard-api.interface.database.contacts-repository
  (:require [portcard-api.interface.database.utils :as utils]
            [clojure.spec.alpha :as s]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [portcard-api.domain.contacts :as contact-model]
            [portcard-api.domain.base :as base-model]))

;; define protocol


(defprotocol Contacts
  (get-contacts [db])
  (get-contact [db uid])
  (get-latest-contacts-by-user [db user-id from])
  (get-contacts-by-user [db user-id])
  (create-contact [db contact])
  (delete-contact [db idm]))

(defn contacts-repository? [inst]
  (satisfies? Contacts inst))

(defn ->contact [contact-db]
  (let [{:keys [uid user_uid subject contact_from contact_from_name created_at]} contact-db
        created_at (utils/sql-to-long created_at)]
    (utils/remove-empty
     {:uid uid
      :to user_uid
      :from contact_from
      :from-name contact_from_name
      :subject subject
      :created_at created_at})))

(defn ->contact-db [contact]
  (let [{:keys [uid to subject from from-name]} contact]
    {:uid uid
     :user_uid to
     :contact_from from
     :contact_from_name from-name
     :subject subject}))

(s/def ::contacts-repository contacts-repository?)

(extend-protocol Contacts
  portcard_api.infrastructure.sql.sql.Boundary
  (get-contacts [{:keys [spec]}]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn ["SELECT * FROM contacts"]
                          {:builder-fn rs/as-unqualified-maps})
           (mapv #(into {} %))
           (map ->contact))))

  (get-contact [{:keys [spec]} uid]
    (->> (utils/get-by-id spec :contacts :uid uid)))

  (get-latest-contacts-by-user [{:keys [spec]} user-id from]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute-one! conn ["SELECT * FROM contacts WHERE user_uid = ? AND contact_from = ? order by created_at desc limit 1" user-id from]
                              {:builder-fn rs/as-unqualified-maps})
           ->contact)))

  (get-contacts-by-user [{:keys [spec]} to]
    (->> (utils/get-by-id spec :contacts :user_uid to)))

  (create-contact [{:keys [spec]} contact]
    {:pre [(s/valid? ::contact-model/creation-contact contact)]}
    (->>
     (->contact-db contact)
     (utils/insert! spec :contacts)))

  (delete-contact [{:keys [spec]} idm]
    {:pre [(s/valid? ::base-model/map idm)]
     :post [(s/valid? ::base-model/boolean %)]}
    (->> (utils/delete! spec :contacts idm)
         zero?
         not)))

;; playground
;; (defn rand-str [len]
;;   (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

;; (defonce inst
;;   (portcard-api.infrastructure.sql.sql/->Boundary
;;    {:datasource
;;     (portcard-api.infrastructure.sql.sql/wrap-logger
;;      (hikari-cp.core/make-datasource
;;       {:jdbc-url (environ.core/env :database-url)}))}))

;; (let [{:keys [spec]} inst]
;;   (with-open [conn (jdbc/get-connection (:datasource spec))]
;;     (let [users (->> (jdbc/execute! conn ["SELECT * FROM users"]
;;                                     {:builder-fn rs/as-unqualified-maps})
;;                      (mapv #(into {} %)))]
;;       (print (:uid (nth users 1))))))
;;
;;(clojure.pprint/pprint (first (get-contacts inst)))

;; (let [contact {:uid (java.util.UUID/randomUUID)
;;                :to "gYbH54n3WGPvz9CEWayIEP2JaKg1"
;;                :subject "Hello, nice to meet you!"
;;                :from "meguru.mokke@gmail.com"}]
;;   (create-contact inst contact))
;; (get-contacts inst)
;; (get-latest-contacts-by-user inst "gYbH54n3WGPvz9CEWayIEP2JaKg1" "meguru.mokke@gmail.com")

;; (let [sample-contact (first (get-contacts inst))]
;;   (= (:uid sample-contact)
;;      (:uid (get-contact inst (:uid sample-contact)))))

;; (let [sample-contact (first (get-contacts inst))]
;;   (count (get-contacts-by-user inst (:to sample-contact))))

;; (delete-contact inst {:user_uid  "gYbH54n3WGPvz9CEWayIEP2JaKg1"})

;; {
;;   "from": "meguru.mokke@gmail.com",
;;   "from-name": "Meguru",
;;   "to": "gYbH54n3WGPvz9CEWayIEP2JaKg1",
;;   "title": "hello",
;;   "body-text": "nice to meet you"
;; }
