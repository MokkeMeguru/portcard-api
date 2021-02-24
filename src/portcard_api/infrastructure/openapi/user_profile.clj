(ns portcard-api.infrastructure.openapi.user-profile
  (:require [clojure.spec.alpha :as s]))

(s/def ::display-name string?)
(s/def ::email string?)
(s/def ::twitter string?)
(s/def ::facebook string?)
(s/def ::contact (s/keys :opt-un [::email ::twitter ::facebook]))
(s/def ::role-category string?)
(s/def ::link-category-name string?)
(s/def ::link-url string?)
(s/def ::primary-rank int?)
(s/def ::role-link (s/keys :req-un [::link-category-name ::link-url]))
(s/def ::role-links (s/* ::role-link))
(s/def ::role (s/keys :req-un [::role-category ::primary-rank] :opt-un [::role-links]))
(s/def ::roles (s/* ::role))
(s/def ::icon-blob string?)

(s/def ::user-id string?)

(s/def ::icon-blob string?)

;; user profile
;; post
(s/def ::post-user-profile-parameters (s/keys :req-un [::display-name] :opt-un [::contact ::roles]))

;; get
(s/def ::get-user-profile-parameters (s/keys :req-un [::user-id]))

(s/def ::get-user-profile-responses (s/keys :req-un [::display-name] :opt-un [::contact ::roles ::icon-blob]))

;; user profile icon
;; get
(s/def ::get-user-profile-icon-parameters (s/keys :req-un [::icon-blob ::user-id]))

;; post
(s/def ::post-user-profile-icon-parameters-path (s/keys :req-un [::user-id]))
