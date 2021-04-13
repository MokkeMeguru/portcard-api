(ns portcard-api.infrastructure.firebase.firebase
  (:import (com.google.firebase FirebaseApp FirebaseOptions)
           (com.google.firebase.database DatabaseReference FirebaseDatabase)
           (com.google.auth.oauth2 GoogleCredentials)
           (com.google.firebase.auth FirebaseAuth))
  (:require [integrant.core :as ig]
            [taoensso.timbre :as timbre]))

(defrecord FirebaseBoundary [firebaseApp])

(defmethod ig/init-key ::firebase
  [_ {:keys [env]}]
  (let [firebase-credentials (:firebase-credentials env)
        ;; firebase-database (:firebase-database env)
        firebase-options (FirebaseOptions/builder)
        firebaseApp (-> firebase-options
                        (.setCredentials firebase-credentials)
                        ;; (.setDatabaseUrl firebase-database)
                        .build
                        FirebaseApp/initializeApp)]
    ;; (timbre/info "connectiong to " firebase-database " for firebase")
    (->FirebaseBoundary firebaseApp)))

(defmethod ig/halt-key! ::firebase
  [_ boundary]
  (->
   boundary
   .firebaseApp
   .delete))

;; for development
;; (def sample (->FirebaseBoundary "sample"))
;; (:credentials sample)
