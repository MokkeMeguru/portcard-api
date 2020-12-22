(ns portcard-api.infrastructure.handler.check-account-name
  (:require [portcard-api.usecase.check-account-name :as check-account-name-usecase]))

(def check-account-name
  {:summary "check-account-name"
   :parameters {:query {:uname string?}}
   :responses {201 {:body {:exist boolean?}}}
   :handler (fn [{:keys [db parameters]}]
              (let [uname (-> parameters :query :uname)]
                (check-account-name-usecase/check-account-name uname db)))})
