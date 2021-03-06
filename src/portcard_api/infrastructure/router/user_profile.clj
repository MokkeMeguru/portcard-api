(ns portcard-api.infrastructure.router.user-profile
  (:require [clojure.walk :as w]
            [portcard-api.infrastructure.handler.user-profile :refer [get-user-profile post-user-profile]]
            [portcard-api.infrastructure.handler.user-profile-icon :refer [get-user-profile-icon post-user-profile-icon]]
            [reitit.ring.middleware.multipart :as multipart]))

(defn user-profile-router [env]
  ["/user-profile"
   {:swagger {:tags ["user profile"]}}
   [""
    {:post post-user-profile}]
   ["/:user-id"
    [""
     {:get get-user-profile}]
    ["/icon"
     [""
      {:post post-user-profile-icon}]
     ["/:icon-blob"
      {:get get-user-profile-icon}]]]])
