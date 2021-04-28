(ns portcard-api.infrastructure.router.registration
  (:require [portcard-api.infrastructure.handler.check-account-name :refer [check-account-name]]
            [portcard-api.infrastructure.handler.signin :refer [signin]]
            [portcard-api.infrastructure.handler.signup :refer [signup]]))

(defn registration-router [env]
  ["/registration"
   {:swagger {:tags ["registration"]}}
   ["/signin"
    {:post signin}]
   ["/signup"
    {:post signup}]
   ["/check-account-name"
    {:get check-account-name}]])
