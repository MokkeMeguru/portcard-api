(ns portcard-api.infrastructure.openapi.users
  (:require [clojure.spec.alpha :as s]))

(s/def ::uid string?)
(s/def ::uname string?)
(s/def ::display-name string?)

(s/def ::user-signup-parrams (s/keys :req-un [::uname]))
(s/def ::user-signup-response (s/keys :req-un [::uname]))
