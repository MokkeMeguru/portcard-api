(ns portcard-api.domain.user-roles
  (:require [clojure.spec.alpha :as s]))

(s/def ::uid uuid?)
(s/def ::category int?)
(s/def ::primary_rank int?)
(s/def ::created_at pos?)
(s/def ::updated_at pos?)
(s/def ::user_uid string?)
(s/def ::empty-map empty?)

(s/def ::user-role (s/keys :req-un [::uid ::category ::primary_rank ::created_at] :opt-un [::updated_at]))

(s/def ::user-roles (s/* ::user-role))
(s/def ::creation-user-role (s/keys :req-un [::uid ::user_uid ::category ::primary_rank]))

(s/def ::link_uid uuid?)
(s/def ::user_role_uid uuid?)
;; (s/def ::link_category int?)
(s/def ::link_category_name string?)
(s/def ::link_blob string?)

(s/def ::user-role-link (s/keys :req-un [::link_uid ::link_category_name ::link_blob ::created_at] :opt-un [::updated_at]))
(s/def ::creation-user-role-link (s/keys :req-un [::uid ::user_role_uid ::link_category_name ::link_blob]))
(s/def ::user-role-links (s/* ::user-role-link))

;; (defn link-category [name]
;;   (condp = name
;;     "github" 1
;;     "pixiv" 2
;;     "niconico_seiga" 3
;;     "niconico_douga" 4
;;     0))

;; (defn decode-link-category [id]
;;   (condp = id
;;     1 "github"
;;     2 "pixiv"
;;     3 "niconico_seiga"
;;     4 "niconico_douga"
;;     0))

(defn role-category [name]
  (condp = name
    "programming" 1
    "illust" 2
    "movie" 3
    "novel" 4
    0))

(defn decode-role-category [id]
  (condp = id
    1 "programming"
    2 "illust"
    3 "movie"
    4 "novel"
    0))
