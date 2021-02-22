(ns portcard-api.interface.database.user-topics-repository
  (:require [portcard-api.interface.database.utils :as utils]
            [portcard-api.domain.user-topics :as user-topic-model]
            [next.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [portcard-api.domain.users :as user-model]
            [portcard-api.domain.base :as base-model]
            [next.jdbc.result-set :as rs]
            [orchestra.spec.test :as st]
            [next.jdbc.sql :as njs]
            [portcard-api.util :as util]))

;; define protocol
(defprotocol UserTopics
  (get-user-topics [db])
  (get-user-topics-chunk [db user-uid topic-from topic-take category order])
  (get-user-topic [db user-uid])
  (get-user-topic-by-id [db topic-id])
  ;; TODO: (get-topic [db uid])
  (create-user-topic [db topic])
  (count-user-topics [db user-id])
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
  (let [{:keys [uid idx user_uid title link description created_at updated_at user_topic_image_blob category]} user-topic-db
        created_at (utils/sql-to-long created_at)
        updated_at (if (-> updated_at nil?) nil (utils/sql-to-long updated_at))]
    (utils/remove-empty
     {:uid uid
      :idx idx
      :user_uid user_uid
      :title title
      :link link
      :description description
      :category category
      :created_at created_at
      :image_blob user_topic_image_blob})))

(defn ->user-topic-db [user-topic]
  (let [{:keys [uid user_uid title description link category]} user-topic]
    (utils/remove-empty
     {:uid uid
      :user_uid user_uid
      :title title
      :link link
      :category category
      :description description})))

(defn ->user-topic-image-db [user-topic]
  (let [{:keys [uid image_blob]} user-topic]
    (utils/remove-empty
     {:user_topic_uid uid
      :user_topic_image_blob image_blob})))

;; implementation


(def sql-basic-selection
  "SELECT * FROM user_topics INNER JOIN user_topic_images ON (user_topics.uid = user_topic_images.user_topic_uid)")

(defn build-get-user-topics-chunk-query [user-uid from take category order]
  {:pre [(string? user-uid)
         (or (nil? from) (int? from))
         (<= 1 take 20)
         (or (nil? category) (int? category))
         (some (partial = order) ["asc" "desc"])]}
  (clojure.string/join
   " "
   (cond-> []
     true (conj sql-basic-selection)
     (some? user-uid) (conj "WHERE user_uid = ?")
     (and (some? from) (= order "asc"))  (conj "AND idx >= ?")
     (and (some? from) (not= order "asc")) (conj "AND idx <= ?")
     (some? category) (conj "AND category = ?")
     (some? order) (conj (str "order by idx " order))
     (some? take) (conj "limit ?"))))

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

  (get-user-topic-by-id [{:keys [spec]} ^java.util.UUID topic-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute-one! conn
                              [(clojure.string/join " " [sql-basic-selection "WHERE user_topics.uid = ?"]) topic-id]
                              {:builder-fn rs/as-unqualified-lower-maps})
           ->user-topic)))

  (get-user-topics-chunk
    [{:keys [spec]} user-uid topic-from topic-take category order] ;; order asc or desc
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (let [query (build-get-user-topics-chunk-query user-uid topic-from topic-take category order)
            raw-topics (jdbc/execute! conn (util/remove-nil [query user-uid topic-from topic-take category])
                                      {:builder-fn rs/as-unqualified-maps})]
        (->>
         raw-topics
         (map #(into {} %))
         (map ->user-topic)))))

  (count-user-topics [{:keys [spec]} user-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (jdbc/execute-one! conn
                         [(clojure.string/join " " ["SELECT COUNT (*) FROM user_topics"])])))

  ;; (get-latest-user-topic [{:keys [spec]} user-id]
  ;;   (with-open [conn (jdbc/get-connection (:datasource spec))]
  ;;     (->> (jdbc/execute-one! conn
  ;;                             [(clojure.string/join " " [sql-basic-selection "where user_uid = ?" "order by user_topics.created_at desc" "limit 1"]) user-id]
  ;;                             {:builder-fn rs/as-unqualified-maps})
  ;;          ->user-topic)))

  (create-user-topic [{:keys [spec]} topic]
    {:pre [(s/valid? ::user-topic-model/creation-user-topic topic)]
     :post [(s/valid? ::user-topic-model/user-topic %)]}
    (let [topic-db (->user-topic-db topic)
          topic-image-db (->user-topic-image-db topic)]
      (with-open [conn (jdbc/get-connection (:datasource spec))]
        (jdbc/with-transaction [tx conn]
          (let [topic-result (njs/insert! tx :user_topics topic-db utils/insert-option)
                topic-image-result (njs/insert! tx :user_topic_images topic-image-db utils/insert-option)]
            (->user-topic (merge topic-result topic-image-result)))))))

  (delete-user-topic [{:keys [spec]} topic-uid]
    {:pre [(s/valid? ::user-topic-model/uid topic-uid)]
     :post [(s/valid? ::base-model/boolean %)]}
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (jdbc/with-transaction [tx conn]
        (let [topic-image-result (:next.jdbc/update-count
                                  (njs/delete! tx :user_topic_images {:user_topic_uid topic-uid}))
              topic-result (:next.jdbc/update-count (njs/delete! tx :user_topics {:uid topic-uid}))]
          (and (-> topic-result zero? not)
               (-> topic-image-result zero? not)))))))


;; (let [user-uid sample-uid
;;       from 0
;;       take 10
;;       category nil
;;       order "desc"]
;;   (build-get-user-topics-chunk-query user-uid from take category order))

;; test

;; (clojure.string/join " " [sql-basic-selection "where user_uid = ?" "order by created_at desc"])


;; (st/instrument)
;; (defonce inst
;;   (portcard-api.infrastructure.sql.sql/->Boundary
;;    {:datasource
;;     (portcard-api.infrastructure.sql.sql/wrap-logger
;;      (hikari-cp.core/make-datasource
;;       {:jdbc-url (environ.core/env :database-url)}))}))

;; (let [uuid (:uid (first (get-user-topics inst)))
;;       recon (java.util.UUID/fromString (.toString uuid))]
;;   [(get-user-topic-by-id inst uuid)
;;    (= uuid recon)])

;; (get-user-topic-by-id inst (java.util.UUID/fromString "e0d17265-6a3b-4f23-a0da-6ac7bf75fd43"))
;; (def sample-uid "b3mXXLoTA1QeLb1UoiknB3eerwn1")
 ;; (get-user-topics inst)
 ;;
;; (count (get-user-topic inst sample-uid))


;; (get-user-topics-chunk inst sample-uid 0 100 nil "desc")


;; (get-user-topics inst)
;; (def sample-uuid (java.util.UUID/fromString "8b6a444b-203a-4447-9b43-c6d1c6409381"))
;; (def sample-uuid2 (java.util.UUID/fromString "8b6a444b-203a-4447-9b43-c6d1c6409382"))


;; (def sample-topic
;;   {:uid sample-uuid
;;    :user_uid sample-uid
;;    :category 1
;;    :link "https://github.com/MokkeMeguru/portcard-api"
;;    :title "new topic"
;;    :image_blob "image-db/sample-image.png"})


-
;; (def sample-topic2
;;   {:uid sample-uuid2
;;    :user_uid sample-uid
;;    :title "new topic2"
;;    :category 0
;;    :description "any something description"
;;    :image_blob "image-db/sample-image2.png"})
;; (s/valid? ::user-topic-model/creation-user-topic sample-topic)

;; (create-user-topic inst sample-topic)

;; (delete-user-topic inst sample-uuid)

;; (create-user-topic inst sample-topic)
;; (create-user-topic inst sample-topic2)

;; (get-user-topics inst)
;; (get-user-topics-chunk inst sample-uid 0 10 nil "asc")
;; (get-user-topics-chunk inst sample-uid 0 10 0 "asc")
;; (get-user-topics-chunk inst sample-uid 0 10 1 "asc")


;; (get-user-topic inst sample-uid)
;; (get-user-topic inst "*")

;; (delete-user-topic inst sample-uuid)
;; (delete-user-topic inst sample-uuid2)

;; (st/unstrument)
