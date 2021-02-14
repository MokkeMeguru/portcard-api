(ns portcard-api.infrastructure.handler.contact
  (:require [clojure.spec.alpha :as s]
            [portcard-api.usecase.post-contact :as post-contact-usecase]
            [clojure.walk :as w]))

(s/def ::id-token string?)
(s/def ::from string?)
(s/def ::to string?)
(s/def ::title string?)
(s/def ::body-text string?)
(s/def ::user-id string?)
(s/def ::post-contact-paths (s/keys :req-un [::user-id]))
(s/def ::post-contact-parameters (s/keys :req-un [::from ::to ::title ::body-text]))

(def post-contact
  {:summary "post contact"
   :swagger {:secutiry [{:Bearer []}]}
   :parameters {:body ::post-contact-parameters}
   :handler (fn [{:keys [parameters headers db gmail-service]}]
              (let [{:keys [body]} parameters
                    [result err] (post-contact-usecase/post-contact body db gmail-service)]
                (cond
                  (not (nil? err)) err
                  :else {:status 201})))})
