(ns portcard-api.infrastructure.router.utils
  (:require [taoensso.timbre :as timbre]))

(defn wrap-db [handler db]
  (fn [request]
    (handler (assoc request :db db))))

(defn wrap-image-db [handler image-db]
  (fn [request]
    (handler (assoc request :image-db image-db))))

(defn wrap-gmail-service [handler gmail-service]
  (fn [request]
    (handler (assoc request :gmail-service gmail-service))))

(defn my-wrap-cors
  "Wrap the server response in a Control-Allow-Origin Header to
  allow connections from the web app."
  [handler]
  (fn [request]
    (timbre/warn "Access Origin: " (-> request :headers (get "origin")))
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Credentials"] "true")
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "authorization,content-type")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "POST,GET,OPTIONS,DELETE,PUT,UPDATE,PATCH")))))
