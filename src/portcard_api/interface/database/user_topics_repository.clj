(ns portcard-api.interface.database.user-topics-repository
  (:require [portcard-api.interface.database.utils :as utils]
            [portcard-api.domain.user-topics :as user-topic-model]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [portcard-api.domain.users :as user-model]
            [portcard-api.domain.base :as base-model]
            [next.jdbc.result-set :as rs]
            [orchestra.spec.test :as st]
            [next.jdbc.sql :as njs]))

;; define protocol
(defprotocol UserTopics
  (get-user-topics [db])
  (get-user-topic [db user-uid])
  ;; TODO: (get-topic [db uid])
  (create-user-topic [db topic])
  (delete-user-topic [db topic-uid]))

(defn user-topics-repository? [inst]
  (satisfies? UserTopics inst))


;; define spec


(s/def ::user-topics-repository user-topics-repository?)
(s/fdef get-user-topics
  :args (s/cat :db ::user-topics-repository)
  :ret ::user-topic-model/user-topics)

(s/fdef get-user-topic
  :args (s/cat :db ::user-topics-repository :user-uid ::user-model/uid)
  :ret ::user-topic-model/user-topics)

(s/fdef create-user-topic
  :args (s/cat :db ::user-topics-repository :topic ::user-topic-model/creation-user-topic)
  :ret ::user-topic-model/user-topic)

(s/fdef delete-user-topic
  :args (s/cat :db ::user-topics-repository :topic-uid ::user-topic-model/uid)
  :ret ::base-model/boolean)

;; TODO: move below code into portcard_api/interface/database/sql/user_topics_repository.clj
;; define model parser


(defn ->user-topic [user-topic-db]
  (let [{:keys [uid title description created_at updated_at user_topic_image_blob]} user-topic-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:uid uid
      :title title
      :description description
      :created_at created_at
      :image_blob user_topic_image_blob})))

(defn ->user-topic-db [user-topic]
  (let [{:keys [uid user_uid title description]} user-topic]
    (utils/remove-empty
     {:uid uid
      :user_uid user_uid
      :title title
      :description description})))

(defn ->user-topic-image-db [user-topic]
  (let [{:keys [uid image_blob]} user-topic]
    (utils/remove-empty
     {:user_topic_uid uid
      :user_topic_image_blob image_blob})))

;; implementation


(def sql-basic-selection
  "SELECT * FROM user_topics INNER JOIN user_topic_images ON (user_topics.uid = user_topic_images.user_topic_uid)")

(def sql-insert-option)
(extend-protocol UserTopics
  portcard_api.infrastructure.sql.sql.Boundary

  (get-user-topics [{:keys [spec]}]
    {:post [(s/valid? ::user-topic-model/user-topics %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn
                          [sql-basic-selection] {:builder-fn rs/as-unqualified-lower-maps})
           (map #(into {} %))
           (map ->user-topic))))

  (get-user-topic [{:keys [spec]} user-uid]
    {:pre [(s/valid? ::user-model/uid user-uid)]
     :post [(s/valid? ::user-topic-model/user-topics %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn
                          [(clojure.string/join " " [sql-basic-selection "WHERE user_uid = ?"]) user-uid]
                          {:builder-fn rs/as-unqualified-lower-maps})
           (map #(into {} %))
           (map ->user-topic))))

  (create-user-topic [{:keys [spec]} topic]
    {:pre [(s/valid? ::user-topic-model/creation-user-topic topic)]
     ;; :post [(s/valid? ::user-topic-model/user-topic %)]
     }
    (let [topic-db (->user-topic-db topic)
          topic-image-db (->user-topic-image-db topic)]
      (with-open [conn (jdbc/get-connection (:datasource spec))]
        (jdbc/with-transaction [tx conn]
          (let [topic-result (njs/insert! tx :user_topics topic-db utils/insert-option)
                topic-image-result (njs/insert! tx :user_topic_images topic-image-db utils/insert-option)]
            (->user-topic (merge topic-result topic-image-result)))))))

  (delete-user-topic [{:keys [spec]} topic-uid]
    {:pre [(s/valid? ::user-topics-model/uid topic-uid)]
     :post [(s/valid? ::base-model/boolean %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (jdbc/with-transaction [tx conn]
        (let [topic-image-result (:next.jdbc/update-count
                                  (njs/delete! tx :user_topic_images {:user_topic_uid topic-uid}))
              topic-result (:next.jdbc/update-count (njs/delete! tx :user_topics {:uid topic-uid}))]
          (and (-> topic-result zero? not)
               (-> topic-image-result zero? not)))))))


;; test
;; (st/instrument)
;; (defonce inst
;;   (portcard-api.infrastructure.sql.sql/->Boundary
;;    {:datasource
;;     (portcard-api.infrastructure.sql.sql/wrap-logger
;;      (hikari-cp.core/make-datasource
;;       {:jdbc-url (environ.core/env :database-url)}))}))

;; (def sample-uid "b3mXXLoTA1QeLb1UoiknB3eerwn1")
;; (get-user-topics inst)
;; (get-user-topic inst sample-uid)
;; (get-user-topic inst "*")

;; (def sample-uuid (java.util.UUID/fromString "8b6a444b-203a-4447-9b43-c6d1c6409381"))
;; (def sample-uuid2 (java.util.UUID/fromString "8b6a444b-203a-4447-9b43-c6d1c6409382"))

;; (def sample-topic
;;   {:uid sample-uuid
;;    :user_uid sample-uid
;;    :title "new topic"
;;    :image_blob "image-db/sample-image.png"})

;; (def sample-topic2
;;   {:uid sample-uuid2
;;    :user_uid sample-uid
;;    :title "new topic2"
;;    :description "any something description"
;;    :image_blob "image-db/sample-image2.png"})

;; (create-user-topic inst sample-topic)
;; (delete-user-topic inst sample-uuid)
;; (create-user-topic inst sample-topic)
;; (create-user-topic inst sample-topic2)

;; (get-user-topics inst)
;; (get-user-topic inst sample-uid)
;; (get-user-topic inst "*")

;; (delete-user-topic inst sample-uuid)
;; (delete-user-topic inst sample-uuid2)
;; (st/unstrument)
