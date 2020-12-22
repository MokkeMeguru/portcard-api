(ns portcard-api.domain.base
  (:require [clojure.spec.alpha :as s]))

(s/def ::boolean boolean?)
(s/def ::keyword keyword?)
(s/def ::string string?)
(s/def ::map map?)
(s/def ::empty empty?)
