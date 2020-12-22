(ns portcard-api.domain.user-topics
  (:require [clojure.spec.alpha :as s]
            [portcard-api.domain.users :as user-model]))

(s/def ::uid uuid?)
(s/def ::user_uid ::user-model/uid)
(s/def ::title string?)
(s/def ::description string?)
(s/def ::created_at pos?)
(s/def ::updated_at pos?)
(s/def ::image_blob string?)

(s/def ::user-topic (s/keys :req-un [::uid ::title ::created_at ::image_blob] :opt-un [::updated_at  ::description]))
(s/def ::creation-user-topic (s/keys :req-un [::uid ::user_uid ::title ::image_blob] :opt-un [::description]))
(s/def ::user-topics (s/* ::user-topic))
