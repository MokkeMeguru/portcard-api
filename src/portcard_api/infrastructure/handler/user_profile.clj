(ns portcard-api.infrastructure.handler.user-profile
  (:require [clojure.walk :as w]
            [clojure.spec.alpha :as s]))

(s/def ::display-name string?)
(s/def ::email string?)
(s/def ::twitter string?)
(s/def ::facebook string?)
(s/def ::contact (s/keys :opt-un [::email ::twitter ::facebook]))
(s/def ::role-category string?)
(s/def ::link-category string?)
(s/def ::link-url string?)
(s/def ::role (s/keys :req-un [::link-category ::link-url]))
(s/def ::roles (s/* ::role))
(s/def ::main-role (s/keys :req-un [::role-category] :opt-un [::roles]))
(s/def ::sub-roles (s/* (s/keys :req-un [::role-category] :opt-un [::roles])))
(s/def ::icon-blob string?)

(def post-user-profile
  {:summary "update or insert user profile"
   :swagger {:security [{:Bearer []}]}
   :parameters {:body
                (s/keys :req-un [::display-name] :opt-un [::contact ::main-role ::sub-roles])}
   :handler (fn [{:keys [parameters headers db]}]
              (let [{:keys [body]} parameters
                    id-token (-> headers w/keywordize-keys :authorization)]
                {:status 201}))})

(def get-user-profile
  {:summary "get user profile"
   :parameters {:path {:user-id string?}}
   :responses {201 {:body
                    (s/keys :req-un [::display-name] :opt-un [::contact ::main-role ::sub-roles])}}
   :handler (fn [{:keys [parameters]}]
              {:status 201})})


;; {
;;   "display-name": "Meguru",
;;   "contact": {
;;     "email": "meguru.mokke@gmail.com",
;;     "twitter": "@MeguruMokke"
;;   },
;;   "main-role": {
;;     "role-category": "programming",
;;     "roles": [
;;       {
;;         "link-category": "Github",
;;         "link-url": "https://github.com/MokkeMeguru"
;;       }
;;     ]
;;   }
;; }
