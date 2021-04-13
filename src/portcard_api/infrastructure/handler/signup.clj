(ns portcard-api.infrastructure.handler.signup
  (:require [portcard-api.domain.errors :as errors]
            [portcard-api.infrastructure.openapi.users :as openapi-users]
            [portcard-api.usecase.signup :as signup-usecase]
            [clojure.walk :as w]))

(defn ->uname [result]
  {:uname (-> result :user :uname)})

(def signup
  {:summary "signup with username and firebase id-token"
   :swagger {:security [{:Bearer []}]}
   :parameters {:body ::openapi-users/signup-parameters}
   :responses {201 {:body ::openapi-users/signup-responses}}
   :handler (fn [{:keys [headers parameters db]}]
              (let [{{:keys [uname]} :body} parameters
                    id-token (-> headers w/keywordize-keys :authorization)
                    [result err]  (signup-usecase/signup uname id-token db)]
                (if
                 (-> err empty? not) err
                 {:status 201
                  :body (->uname result)})))})
