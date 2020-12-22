(ns portcard-api.interface.firebase.auth
  (:import (com.google.firebase FirebaseApp FirebaseOptions)
           (com.google.firebase.database DatabaseReference FirebaseDatabase)
           (com.google.auth.oauth2 GoogleCredentials)
           (com.google.firebase.auth FirebaseAuth))
  (:require [clojure.java.io :as io]
            [clojure.walk :as w]
            [portcard-api.util :refer [err->>]]
            [portcard-api.domain.errors :as errors]))

(defprotocol Auth
  (get-user-uid [id-token]))

(defn decode-token [id-token]
  (-> (FirebaseAuth/getInstance)
      (.verifyIdToken id-token)
      .getUid))

(defn expired-id-token? [cause]
  (if (clojure.string/includes? cause "expired")
    [nil errors/expired-id-token]
    [cause nil]))

(defn invalid-id-token? [cause]
  (if (clojure.string/includes? cause "Failed to parse")
    [nil errors/invalid-id-token]
    [cause nil]))

(defn unknown-id-token? [cause]
  [nil (errors/unknown-error "the firebase token is something wrong")])

(defn safe-decode-token [id-token]
  (try
    {:result :succcess
     :user-id (decode-token id-token)
     :cause nil}
    (catch Exception e
      {:result :failure
       :user-id nil
       :cause
       (second
        (err->>
         (.getMessage e)
         expired-id-token?
         invalid-id-token?
         unknown-id-token?))})))


;; (def idtoken
;;   "eyJhbGciOiJSUzI1NiIsImtpZCI6IjY5NmFhNzRjODFiZTYwYjI5NDg1NWE5YTVlZTliODY5OGUyYWJlYzEiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiTWVndXJ1IiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hLS9BT2gxNEdoeWhoSDZ6VmdHMnV1Szh0SWRxWXZVcWQ4UG1fM2hHQkpZVDFTMW13PXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL2F1dGgtZXhhbXBsZS1iNGJlOSIsImF1ZCI6ImF1dGgtZXhhbXBsZS1iNGJlOSIsImF1dGhfdGltZSI6MTYwNzg4MDEyNywidXNlcl9pZCI6ImIzbVhYTG9UQTFRZUxiMVVvaWtuQjNlZXJ3bjEiLCJzdWIiOiJiM21YWExvVEExUWVMYjFVb2lrbkIzZWVyd24xIiwiaWF0IjoxNjA3ODgwMTI4LCJleHAiOjE2MDc4ODM3MjgsImVtYWlsIjoibWVndXJ1Lm1va2tlQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJmaXJlYmFzZSI6eyJpZGVudGl0aWVzIjp7Imdvb2dsZS5jb20iOlsiMTEwMzg3ODY1NDU1NDIwOTQyODE4Il0sImVtYWlsIjpbIm1lZ3VydS5tb2trZUBnbWFpbC5jb20iXX0sInNpZ25faW5fcHJvdmlkZXIiOiJnb29nbGUuY29tIn19.dxCVRP-Iu8Gj3slvjTVACzb5a2MojK6t-lgTdx4mTY4Eiy_YPZPyDUIkEoecWxjxr66etbkv7vZbdTFN2ZV5pBkNxQJ5vwhf9KbmHssZBBgMRHc4gLlkSoKwbmHWWj-E0mne1WDSO0rMn8YlqSy98bUqYubG_sqEAUb-I8nMBnVSCMfYCFdUFbvEBoEMRYo0eivf719LdXvFVj5z7YXc_EvwBt1xK6WssLGTeNG5bKOmB_slstV7KVDYtX2lL9wBOqkFjfvtLEgS9uSEsDM7XUUuUfUMi3xvFgNaK_KJyxfbCOSjrIXZzg-fbmqScJP8J0kajfm4JO_MFYx0EKESPQ")

;; (safe-decode-token idtoken)
;; (safe-decode-token "Hello")
