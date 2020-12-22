(ns portcard-api.infrastructure.router.user-profile
  (:require [clojure.walk :as w]
            [reitit.ring.middleware.multipart :as multipart]
            [portcard-api.infrastructure.handler.user-profile-icon :refer [get-user-profile-icon post-user-profile-icon]]
            [portcard-api.infrastructure.handler.user-profile :refer [post-user-profile get-user-profile]]))

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
