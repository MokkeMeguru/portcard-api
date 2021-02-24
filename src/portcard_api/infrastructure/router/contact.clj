(ns portcard-api.infrastructure.router.contact
  (:require [portcard-api.infrastructure.handler.contact :refer [post-contact]]))

(defn contact-router [env]
  ["/contact"
   {:swagger {:tags ["contact"]}}
   [""
    {:post post-contact}]])
