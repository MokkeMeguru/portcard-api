(ns portcard-api.infrastructure.handler.user-profile-icon
  (:require [reitit.ring.middleware.multipart :as multipart]))

(def get-user-profile-icon
  {:summary "get user icon"
   :parameters {:path {:icon-blob string? :user-id string?}}
   :handler (fn [_]
              {:status 200})})

(def post-user-profile-icon
  {:summary "post user icon"
   :parameters {:multipart
                {:file multipart/temp-file-part}
                :path {:user-id string?}}
   :responses {200 {:body {:file-id string?}}}
   :handler (fn [{:keys [parameters db]}]
              (let [{{:keys [file]} :multipart} parameters

                    fin (:tempfile file)
                    fout-name (.toString (java.util.UUID/randomUUID))]
                (-> fin
                    .toURL
                    .openConnection
                    .getContentType
                    println)
                {:status 200
                 :body {:file-id fout-name}}))})
