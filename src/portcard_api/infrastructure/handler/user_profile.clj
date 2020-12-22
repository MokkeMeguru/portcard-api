(ns portcard-api.infrastructure.handler.user-profile
  (:require [clojure.walk :as w]
            [clojure.spec.alpha :as s]
            [portcard-api.usecase.upsert-user-profile :as upsert-user-profile-usecase]
            [taoensso.timbre :as timbre]))

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
                    {:keys [display-name contact roles]} body]
                (timbre/info (upsert-user-profile-usecase/upsert-user-profile id-token display-name contact roles db))
                {:status 201}))})

(def get-user-profile
  {:summary "get user profile"
   :parameters {:path {:user-id string?}}
   :responses {201 {:body
                    (s/keys :req-un [::display-name] :opt-un [::contact ::roles])}}
   :handler (fn [{:keys [parameters]}]
              {:status 201})})

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
