(ns portcard-api.infrastructure.gmail.gmail
  (:require [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [portcard-api.domain.gmail :as gmail-domain]
            [taoensso.timbre :as timbre])
  (:import [com.google.api.services.gmail GmailScopes]
           [com.google.api.client.json JsonFactory]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.http.javanet NetHttpTransport]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.client.extensions.java6.auth.oauth2 AuthorizationCodeInstalledApp]
           [com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeFlow]
           [com.google.api.client.googleapis.auth.oauth2 GoogleAuthorizationCodeFlow$Builder]
           [com.google.api.client.util.store FileDataStoreFactory]
           [com.google.api.client.googleapis.auth.oauth2 GoogleClientSecrets]
           [com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver]
           [com.google.api.services.gmail Gmail$Builder Gmail]
           [java.util Properties]
           [javax.mail.internet MimeMessage]
           [javax.mail Session]
           [javax.mail.internet InternetAddress]
           [javax.mail Message$RecipientType]
           [org.apache.commons.codec.binary Base64]
           [com.google.api.services.gmail.model Message]
           [com.google.api.client.auth.oauth2 Credential]
           [com.google.api.services.gmail.model Label]))

(def json-factory (JacksonFactory/getDefaultInstance))
(def http-transport (GoogleNetHttpTransport/newTrustedTransport))
(def charset "utf-8")
(def encode "base64")

(def scopes [GmailScopes/GMAIL_LABELS
             GmailScopes/GMAIL_SEND])

(defrecord Boundary [spec])

(s/fdef get-credential)

(defn get-credential "
  get credential info from credential-file
  and then, save token from google

  ```clojure
  (def json-factory (JacksonFactory/getDefaultInstance)):
  (def http-transport (GoogleNetHttpTransport/newTrustedTransport))
  (def scopes [GmailScopes/GMAIL_LABELS
               GmailScopes/GMAIL_SEND])
  (def credential-file (io/resource \"credential.json\"))
  (def tokens-dir (io/resource \"tokens\"))
  (get-credential credential-file tokens-dir scopes)
  ```
  "
  [credential-file tokens-dir scopes]
  (with-open [in (io/input-stream credential-file)]
    (let [secrets (GoogleClientSecrets/load json-factory (java.io.InputStreamReader. in))
          file-data-store-factory (FileDataStoreFactory. (io/file tokens-dir))
          flow (.. (GoogleAuthorizationCodeFlow$Builder. http-transport json-factory secrets scopes)
                   (setDataStoreFactory file-data-store-factory)
                   (setAccessType "offline")
                   build)]
      (-> flow
          (AuthorizationCodeInstalledApp. (LocalServerReceiver.))
          (.authorize "user")))))

(defmethod ig/init-key ::gmail
  [_ {:keys [env]}]
  (let [credential-file (:gmail-credential-file env)
        tokens-dir (:gmail-tokens-dir env)
        gmail-host-addr (:gmail-host-addr env)
        credential (get-credential credential-file tokens-dir scopes)]
    (timbre/info "load gmail credential ...")
    (timbre/info "credential file: " credential-file)
    (timbre/info "tokens directory: " tokens-dir)
    (timbre/info "gmail host address: " gmail-host-addr)
    (->Boundary {:credential credential
                 :gmail-host-addr gmail-host-addr})))
