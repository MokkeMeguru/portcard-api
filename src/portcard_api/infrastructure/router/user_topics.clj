(ns portcard-api.infrastructure.router.user-topics
  (:require [portcard-api.infrastructure.handler.user-topics
             :refer [delete-user-topic get-user-topic-capture get-user-topics get-user-topics-latest post-user-topic]]))

(defn user-topics-router [env]
  ["/user-topics"
   {:swagger {:tags ["user topics"]}}
   [""
    {:post post-user-topic}]
   ["/:user-id"
    [""
     {:get get-user-topics}]
    ["/latest"
     {:get get-user-topics-latest}]
    ["/topic"
     ["/:topic-id"
      [""
       {:delete delete-user-topic}]
      ["/capture"
       ["/:image-blob"
        {:get get-user-topic-capture}]]]]]])
