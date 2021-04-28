(ns portcard-api.infrastructure.server
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]
            [environ.core :as environ]
            [taoensso.timbre :as timbre]))

(defmethod ig/init-key ::server [_ {:keys [env router port]}]
  (let [_port (if-let [_port (environ/env :port)] (Integer/parseInt _port) nil)
        port (or _port port)]
    (timbre/info "server is running in port" port)
    (timbre/info "router is " router)
    (jetty/run-jetty router {:port port
                             :join? false})))

(defmethod ig/halt-key! ::server [_ server]
  (.stop server))
