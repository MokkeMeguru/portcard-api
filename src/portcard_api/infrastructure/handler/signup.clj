(ns portcard-api.infrastructure.handler.signup
  (:require [portcard-api.infrastructure.openapi.users :as openapi-users]
            [portcard-api.usecase.signup :as signup-usecase]
            [clojure.walk :as w]))

(def signup
  {:summary "signup with username and firebase id-token"
   :swagger {:security [{:Bearer []}]}
   :parameters {:body {:uname string?}}
   :responses {201 {:body {:uname string?}}}
   :handler (fn [{:keys [headers parameters db]}]
              (let [{{:keys [uname]} :body} parameters
                    id-token (-> headers w/keywordize-keys :authorization)]
                (signup-usecase/signup uname id-token db)))})
