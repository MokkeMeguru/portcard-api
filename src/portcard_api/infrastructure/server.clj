(ns portcard-api.infrastructure.server
  (:require
   [taoensso.timbre :as timbre]
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty]))

(defmethod ig/init-key ::server [_ {:keys [env router port]}]
  (timbre/info "server is running in port" port)
  (timbre/info "router is " router)
  (jetty/run-jetty router {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (.stop server))
