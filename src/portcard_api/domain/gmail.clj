(ns portcard-api.domain.gmail
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s])
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

(def email-regex  #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(def net-http-transport? (partial instance? NetHttpTransport))
(def satisfy-email-regex? (partial re-matches email-regex))
(def gmail-api-scope? string?)
(def url? (partial instance? java.net.URL))
(def file-exist? #(-> % io/as-file .exists))
(def credential? (partial instance? Credential))
(def gmail-service? (partial instance? Gmail))
(def mime-message? (partial instance? MimeMessage))
(def gmail-message? (partial instance? Message))
(def google-auth-code-flow? (partial instance? GoogleAuthorizationCodeFlow))

;; resource files
(s/def ::credential-file (s/and url? file-exist?))
(s/def ::tokens-dir (s/and url? file-exist?))

;; google api's settings
(s/def ::http-transport net-http-transport?)
(s/def ::scope gmail-api-scope?)
(s/def ::scopes (s/coll-of ::scope))
(s/def ::application-name string?)
(s/def ::service gmail-service?)
(s/def ::port (s/and int? #(<= 0 % 0x10000)))
(s/def ::auth-code-flow google-auth-code-flow?)

(s/def ::credential credential?)
(s/def ::user-id string?)
(s/def ::gmail-label (partial instance? Label))
(s/def ::gmail-labels (s/coll-of ::gmail-label))
(s/def ::gmail-message gmail-message?)

(s/def ::id string?)
(s/def ::label-id string?)
(s/def ::label-ids (s/coll-of ::label-id))
(s/def ::thread-id string?)
(s/def ::gmail-response (s/keys :req-un [::id ::label-ids ::thread-id]))

;; email contents
(s/def ::address (s/and string? satisfy-email-regex?))

(s/def ::to ::address)
(s/def ::from ::address)
(s/def ::cc ::address)
(s/def ::subject string?)
(s/def ::message string?)

(s/def ::mail-message (s/keys :req-un [::to ::from ::subject ::message]))

(s/def ::mime-message mime-message?)

;; infrastructure
(s/def ::gmail-host-addr ::from)
(s/def ::boundary-service
  (s/keys :req-un [::credential
                   ::gmail-host-addr]))
