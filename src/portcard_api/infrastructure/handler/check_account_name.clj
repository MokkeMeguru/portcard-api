(ns portcard-api.infrastructure.handler.check-account-name
  (:require [portcard-api.usecase.check-account-name :as check-account-name-usecase]
            [portcard-api.infrastructure.openapi.users :as openapi-users]))

(defn ->account-name-exist? [{:keys [user-exist]}]
  {:exist user-exist})

(def check-account-name
  {:summary "check-account-name"
   :parameters {:query ::openapi-users/check-account-name-parameters}
   :responses {201 {:body ::openapi-users/check-account-name-responses}}
   :handler (fn [{:keys [db parameters]}]
              (let [uname (-> parameters :query :uname)
                    [user-exist err] (check-account-name-usecase/check-account-name uname db)]
                (if (nil? err)
                  {:status 201
                   :body (->account-name-exist? user-exist)}
                  err)))})
