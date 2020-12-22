(ns portcard-api.infrastructure.handler.signin
  (:require [clojure.walk :as w]
            [portcard-api.usecase.signin :as signin-usecase]))

(def signin
  {:summary "signin"
   :swagger {:security [{:Bearer []}]}
   :responses {201 {:body {:uname string?}}}
   :handler (fn [{:keys [headers db]}]
              (let [id-token (-> headers w/keywordize-keys :authorization)]
                (signin-usecase/signin id-token db)))})
