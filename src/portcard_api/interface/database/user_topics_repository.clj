(ns portcard-api.interface.database.user-topics-repository
  (:require [portcard-api.interface.database.utils :as utils]
            [portcard-api.domain.user-topics :as user-topics-model]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [portcard-api.domain.users :as user-model]
            [portcard-api.domain.base :as base-model]
            [next.jdbc.result-set :as rs]
            [orchestra.spec.test :as st]))

;; define protocol
(defprotocol UserTopics
  (get-user-topics [db])
  (get-user-topic [db user-uid])
  ;; TODO: (get-topic [db uid])
  (create-user-topic [db topic])
  (delete-user-topic [db topic-uid]))

(defn user-topics-repository? [inst]
  (satisfies? UserTopics inst))

;; define model parser


(defn ->user-topic [user-topic-db]
  (let [{:keys [uid title description created_at updated_at image_blob]} user-topic-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:uid uid
      :title title
      :description description
      :created_at created_at
      :image_blob image_blob})))

;; define spec
(s/def ::user-topics-repository user-topics-repository?)
(s/fdef get-user-topics
  :args (s/cat :db ::user-topics-repository)
  :ret ::user-topics-model/user-topics)

(s/fdef get-user-topic
  :args (s/cat :db ::user-topics-repository :user-uid ::user-model/uid)
  :ret ::user-topics-model/user-topics)

(s/fdef create-user-topic
  :args (s/cat :db ::user-topics-repository :topic ::user-topics-model/creation-user-topic)
  :ret ::user-topics-model/user-topic)

(s/fdef delete-user-topic
  :args (s/cat :db ::user-topics-repository :topic-uid ::user-topics-model/uid)
  :ret ::base-model/boolean)


;; implementation


(def sql-basic-selection
  "SELECT * FROM user_topics INNER JOIN user_topic_images ON (user_topics.uid = user_topic_images.user_topic_uid)")

(extend-protocol UserTopics
  portcard_api.infrastructure.sql.sql.Boundary

  (get-user-topics [{:keys [spec]}]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn
                          [sql-basic-selection] {:builder-fn rs/as-unqualified-lower-maps})
           (mapv #(into {} %))
           (map ->user-topic))))

  (get-user-topic [{:keys [spec]} user-uid]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn
                          [(clojure.string/join " " [sql-basic-selection "WHERE user_uid = ?"]) user-uid])))))


;; test


(st/instrument)
(defonce inst
  (portcard-api.infrastructure.sql.sql/->Boundary
   {:datasource
    (portcard-api.infrastructure.sql.sql/wrap-logger
     (hikari-cp.core/make-datasource
      {:jdbc-url (environ.core/env :database-url)}))}))

(def sample-uid "b3mXXLoTA1QeLb1UoiknB3eerwn1")
(get-user-topics inst)
(get-user-topic inst sample-uid)
(get-user-topic inst "*")
