(ns portcard-api.infrastructure.handler.user-profile
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as w]
            [portcard-api.domain.user-roles :as user-roles-model]
            [portcard-api.infrastructure.openapi.user-profile :as openapi-user-profile]
            [portcard-api.interface.database.utils :as utils]
            [portcard-api.usecase.get-user-profile :as get-user-profile-usecase]
            [portcard-api.usecase.upsert-user-profile :as upsert-user-profile-usecase]
            [portcard-api.util :as util]
            [taoensso.timbre :as timbre]))

(defn ->profile-role-link [{:keys [link_category_name link_blob]}]
  (let
   [role-link
    (utils/remove-empty
     {:link-category-name link_category_name
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

(def post-user-profile
  {:summary "update or insert user profile"
   :swagger {:security [{:Bearer []}]}
   :parameters {:body ::openapi-user-profile/post-user-profile-parameters}
   :handler (fn [{:keys [parameters headers db]}]
              (let [{:keys [body]} parameters
                    {:keys [display-name contact roles]} body
                    id-token (-> headers w/keywordize-keys :authorization)
                    [_ err] (upsert-user-profile-usecase/upsert-user-profile id-token display-name contact roles db)]
                (if (nil? err)
                  {:status 201}
                  err)))})

(def get-user-profile
  {:summary "get user profile"
   :parameters {:path ::openapi-user-profile/get-user-profile-parameters}
   :responses {201 {:body ::openapi-user-profile/get-user-profile-responses}}
   :handler (fn [{:keys [parameters db]}]
              (let [{{:keys [user-id]} :path} parameters
                    [profile err] (get-user-profile-usecase/get-user-profile user-id db)]
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
