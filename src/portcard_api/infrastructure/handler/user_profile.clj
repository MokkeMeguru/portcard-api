(ns portcard-api.infrastructure.handler.user-profile
  (:require [clojure.walk :as w]
            [clojure.spec.alpha :as s]
            [portcard-api.usecase.upsert-user-profile :as upsert-user-profile-usecase]
            [portcard-api.usecase.get-user-profile :as get-user-profile-usecase]
            [taoensso.timbre :as timbre]
            [portcard-api.util :as util]
            [portcard-api.interface.database.utils :as utils]
            [portcard-api.domain.user-roles :as user-roles-model]))

(s/def ::display-name string?)
(s/def ::email string?)
(s/def ::twitter string?)
(s/def ::facebook string?)
(s/def ::contact (s/keys :opt-un [::email ::twitter ::facebook]))
(s/def ::role-category string?)
(s/def ::link-category string?)
(s/def ::link-url string?)
(s/def ::primary-rank int?)
(s/def ::role-link (s/keys :req-un [::link-category ::link-url]))
(s/def ::role-links (s/* ::role-link))
(s/def ::role (s/keys :req-un [::role-category ::primary-rank] :opt-un [::role-links]))
(s/def ::roles (s/* ::role))
(s/def ::icon-blob string?)

(def post-user-profile
  {:summary "update or insert user profile"
   :swagger {:security [{:Bearer []}]}
   :parameters {:body
                (s/keys :req-un [::display-name] :opt-un [::contact ::roles])}
   :handler (fn [{:keys [parameters headers db]}]
              (let [{:keys [body]} parameters
                    id-token (-> headers w/keywordize-keys :authorization)
                    {:keys [display-name contact roles]} body
                    [_ err] (upsert-user-profile-usecase/upsert-user-profile id-token display-name contact roles db)]
                (if (nil? err)
                  {:status 201}
                  err)))})
(defn ->profile-role-link [{:keys [link_category link_blob]}]
  (let
   [role-link
    (utils/remove-empty
     {:link-category (user-roles-model/decode-link-category link_category)
      :link-url link_blob})]
    (if (empty? role-link) nil role-link)))

(defn ->profile-role [{:keys [primary_rank category role-links] :as role-usecase}]
  (let
   [role
    (utils/remove-empty
     {:role-links (util/remove-nil (map ->profile-role-link role-links))
      :role-category (user-roles-model/decode-role-category category)
      :primary-rank primary_rank})]
    (if (empty? role) nil role)))

(defn ->profile-contact [{:keys [email twitter facebook] :as contact-usecase}]
  (let [contact (util/remove-empty
                 {:email email
                  :twitter twitter
                  :facebook facebook})]
    (if (empty? contact) nil contact)))

(defn ->profile [{:keys [display-name icon_blob contact roles]}]
  (util/remove-empty
   {:display-name display-name
    :icon-blob icon_blob
    :contact (->profile-contact contact)
    :roles (util/remove-nil (mapv ->profile-role roles))}))

(def get-user-profile
  {:summary "get user profile"
   :parameters {:path {:user-id string?}}
   :responses {201 {:body
                    (s/keys :req-un [::display-name] :opt-un [::contact ::roles ::icon-blob])}}
   :handler (fn [{:keys [parameters db]}]
              (let [{{:keys [user-id]} :path} parameters
                    [profile err] (get-user-profile-usecase/get-user-profile user-id db)]
                (println (keys profile))
                (println (->profile profile))
                (if (nil? err)
                  {:status 201
                   :body (->profile profile)}
                  err)))})

;; -> save display name  user-repository
;; -> save contact       update contect-repository
;; -> save main role     main-role-repository
;; -> save roles         user role links repository
;;
;;
;; {
;;   "display-name": "Meguru",
;;   "contact": {
;;     "email": "meguru.mokke@gmail.com",
;;     "twitter": "@MeguruMokke"
;;   },
;;   "roles": [
;;     {
;;       "role-category": "programming",
;;       "primary-rank": 0,
;;       "role-links": [
;;         {
;;           "link-category": "Github",
;;           "link-url": "https://github.com/MeguruMokke"
;;         }
;;       ]
;;     }
;;   ]
;; }
