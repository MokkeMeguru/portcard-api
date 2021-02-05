(ns portcard-api.infrastructure.openapi.users
  (:require [clojure.spec.alpha :as s]))

(s/def ::uid string?)
(s/def ::uname string?)
(s/def ::display-name string?)

(s/def ::user-signup-parrams (s/keys :req-un [::uname]))
(s/def ::user-signup-response (s/keys :req-un [::uname]))

(s/def ::exist boolean?)

;; signin
(s/def ::signin-responses (s/keys :req-un [::uname]))

;; signup
(s/def ::signup-parameters (s/keys :req-un [::uname]))
(s/def ::signup-responses (s/keys :req-un [::uname]))

;; check acccount name
(s/def ::check-account-name-parameters (s/keys :req-un [::uname]))
(s/def ::check-account-name-responses (s/keys :req-un [::exist]))
