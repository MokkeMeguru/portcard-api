(ns portcard-api.domain.contacts
  (:require [portcard-api.domain.users :as user-model]
            [clojure.spec.alpha :as s]))

(def email-regex  #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(def satisfy-email-regex? (partial re-matches email-regex))

(s/def ::uid uuid?)
(s/def ::to ::user-model/uid)
(s/def ::subject
  (s/and string?
         #(<= 5 (count %) 200)))
(s/def ::from (s/and string? satisfy-email-regex?))

(s/def ::creation-contact
  (s/keys :req-un [::uid ::to ::subject ::from]))
