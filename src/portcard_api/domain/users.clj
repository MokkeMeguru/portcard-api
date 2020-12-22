(ns portcard-api.domain.users
  (:require [clojure.spec.alpha :as s]))

(s/def ::uname string?)
(s/def ::uid string?)
(s/def ::created_at pos?)
(s/def ::updated_at pos?)
(s/def ::display_name string?)
(s/def ::is_deleted boolean?)
(s/def ::empty-map empty?)

(s/def ::user (s/keys :req-un [::uname ::uid ::created_at ::display_name ::is_deleted] :opt-up [::updated_at]))
(s/def ::creation-user (s/keys :req-un [::uname ::uid ::display_name]))
(s/def ::users (s/* ::user))

(s/def ::user_uid ::uid)
(s/def ::email string?)
(s/def ::twitter string?)
(s/def ::facebook string?)

(s/def ::user-profiles-contact (s/keys :req-un [::created_at] :opt-un [::email ::twitter ::facebook ::updated_at]))
(s/def ::creation-user-profiles-contact (s/keys :req-un [::user_uid] :opt-un [::email ::twitter ::facebook]))
(s/def ::user-profiles-contacts (s/* ::user-profiles-contact))

(s/def ::icon_blob string?)

(s/def ::user-profiles-icon (s/keys :req-un [::icon_blob ::created_at] :opt-un [::updated_at]))
(s/def ::creation-user-profiles-icon (s/keys :req-un [::user_uid ::icon_blob]))
(s/def ::user-profiles-icons (s/* ::user-profiles-icon))
