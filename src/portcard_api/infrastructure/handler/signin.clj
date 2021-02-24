(ns portcard-api.infrastructure.handler.signin
  (:require [clojure.walk :as w]
            [portcard-api.usecase.signin :as signin-usecase]
            [portcard-api.infrastructure.openapi.users :as openapi-users]))

(defn ->uname [result]
  {:uname (-> result :user :uname)})

(def signin
  {:summary "signin"
   :swagger {:security [{:Bearer []}]}
   :responses {201 {:body ::openapi-users/signin-responses}}
   :handler (fn [{:keys [headers db]}]
              (let [id-token (-> headers w/keywordize-keys :authorization)
                    [result err] (signin-usecase/signin id-token db)]
                (if (nil? err)
                  {:status 201
                   :body (->uname result)}
                  err)))})
