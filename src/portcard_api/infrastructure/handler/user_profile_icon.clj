(ns portcard-api.infrastructure.handler.user-profile-icon
  (:require [reitit.ring.middleware.multipart :as multipart]
            [portcard-api.usecase.save-user-profile-icon :as save-user-profile-icon-usecase]
            [portcard-api.usecase.get-user-icon :as get-user-icon-usecase]
            [portcard-api.infrastructure.openapi.user-profile :as openapi-user-profile]
            [clojure.walk :as w]
            [clojure.java.io :as io]))

(def get-user-profile-icon
  {:summary "get user icon"
   :parameters {:path ::openapi-user-profile/get-user-profile-icon-parameters}
   :handler (fn [{:keys [parameters db image-db]}]
              (let [icon-blob (-> parameters :path :icon-blob)
                    uname (-> parameters :path :user-id)
                    [{{:keys [image-stream]} :icon} err] (get-user-icon-usecase/get-user-icon uname icon-blob db image-db)]
                (if (nil? err)
                  {:status 200
                   :headers {"Content-type" "image/png"}
                   :body image-stream}
                  err)))})

(def post-user-profile-icon
  {:summary "post user icon"
   :swagger {:security [{:Bearer []}]}
   :parameters {:multipart
                {:file multipart/temp-file-part}
                :path ::openapi-user-profile/post-user-profile-icon-parameters-path}
   :responses {200 {:body {:file-id string?}}}
   :handler (fn [{:keys [parameters db image-db headers]}]
              (let [{{:keys [file]} :multipart} parameters
                    id-token (-> headers w/keywordize-keys :authorization)
                    [{:keys [icon_blob]} err]
                    (save-user-profile-icon-usecase/save-user-profile-icon
                     {:icon-image-stream (io/input-stream (:tempfile file))
                      :image-db image-db
                      :db db
                      :id-token id-token})]
                (if (nil? err)
                  {:status 200
                   :body {:file-id icon_blob}}
                  err)))})
;; post
;; generate rand-str 15
;; detect content type png or jpeg
;; add type end of line
;; center crop the image
;; truncate max 512 x 512
;; save the file into image-db/
;; save uid image-blob into db
;; previous exist
;; delete them

;; get
;; TODO: how to get image (rest api or statics)
;; get uid
;; return image-blob

;; (def chars
;;   (map char (concat (range 48 58) (range 66 92) (range 97 123))))

;; (defn rand-str [len]
;;   (apply str (take len (repeatedly #(nth chars (rand (count chars)))))))

;; (rand-str 15)
;; (count chars)
;; (char (+ 65 25))
;; (char (- 65 25))
