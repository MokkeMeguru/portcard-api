(ns portcard-api.domain.user-topics
  (:require [portcard-api.domain.users :as user-model]
            [clojure.spec.alpha :as s]))

(s/def ::uid uuid?)
(s/def ::user_uid ::user-model/uid)
(s/def ::title string?)
(s/def ::description string?)
(s/def ::created_at pos?)
(s/def ::updated_at pos?)
(s/def ::image_blob string?)

(def http-url-regex #"^https?:\/\/[\w\-\.\/\?\,\#\:\u3000-\u30fe\u4e00-\u9fa0\uff01-\uffe3]+")
(def http-url? (partial re-matches http-url-regex))
(s/def ::link (s/and string? http-url?))

(s/def ::category (s/and int? #(<= 0 %)))

(s/def ::user-topic (s/keys :req-un [::uid ::title ::link ::created_at  ::category ::image_blob] :opt-un [::updated_at  ::description]))
(s/def ::creation-user-topic (s/keys :req-un [::uid ::user_uid ::category ::title ::link ::image_blob] :opt-un [::description]))
(s/def ::user-topics (s/* ::user-topic))
