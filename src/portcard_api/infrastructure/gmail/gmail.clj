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
           [com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver LocalServerReceiver$Builder]
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

;; https://www.cdatablog.jp/entry/gcprefreshtokengrant

(def application-name "Portcard API")
(def json-factory (JacksonFactory/getDefaultInstance))
(def http-transport (GoogleNetHttpTransport/newTrustedTransport))

(def scopes [GmailScopes/GMAIL_LABELS
             GmailScopes/GMAIL_SEND])

(def new-credential-port 8090)

(defrecord Boundary [spec])

(defn- get-auth-code-flow
  [credential-file tokens-dir scopes]
  {:pre [(s/valid? ::gmail-domain/credential-file credential-file)
         (s/valid? ::gmail-domain/tokens-dir tokens-dir)]
   :post [(s/valid? ::gmail-domain/auth-code-flow %)]}
  (with-open [in (io/input-stream credential-file)]
    (let [secrets (GoogleClientSecrets/load json-factory (java.io.InputStreamReader. in))
          file-data-store-factory (FileDataStoreFactory. (io/file tokens-dir))]
      (.. (GoogleAuthorizationCodeFlow$Builder. http-transport json-factory secrets scopes)
          (setDataStoreFactory file-data-store-factory)
          (setAccessType "offline")
          (setApprovalPrompt "force")
          build))))


;; (s/explain ::gmail-domain/credential-file (io/as-url (io/file "resources/credentials.json")))


(defn get-new-credential
  [credential-file tokens-dir scopes port]
  {:pre [(s/valid? ::gmail-domain/credential-file credential-file)
         (s/valid? ::gmail-domain/tokens-dir tokens-dir)
         (s/valid? ::gmail-domain/port port)]
   :post [(s/valid? ::gmail-domain/credential %)]}
  (let [flow (get-auth-code-flow credential-file tokens-dir scopes)
        local-server-receiver   (.. (LocalServerReceiver$Builder.)
                                    (setPort port)
                                    build)
        credential (-> flow
                       (AuthorizationCodeInstalledApp.
                        local-server-receiver)
                       (.authorize "user"))]
    (println "refresh token: " (.getRefreshToken credential))
    (println "expires in seconds: " (.getExpiresInSeconds credential))
    credential))

(defn get-credential "
  get credential info from credential-file and stored secret file
  if secret file is expired or some thing wrong, you need to generate new secret file.

  ```clojure
  (def http-transport (GoogleNetHttpTransport/newTrustedTransport))
  (def scopes [GmailScopes/GMAIL_LABELS
               GmailScopes/GMAIL_SEND])

  (def credential-file (io/resource \"credential.json\"))
  (def tokens-dir (io/resource \"tokens\"))

  (get-credential credential-file tokens-dir scopes)
  ```
  "
  [credential-file tokens-dir scopes]
  {:pre [(s/valid? ::gmail-domain/credential-file credential-file)
         (s/valid? ::gmail-domain/tokens-dir tokens-dir)]
   :post [(s/valid? ::gmail-domain/credential %)]}
  (let [flow (get-auth-code-flow credential-file tokens-dir scopes)
        credential (-> flow (.loadCredential "user"))
        credential (cond
                     (nil? credential) nil
                     (or (some? (.getRefreshToken credential))
                         (nil? (.getExpiresInSeconds credential))
                         (> (.getExpiresInSeconds credential) 60)) credential
                     :else nil)]
    (when (nil? credential)
      (throw (Exception. "credential file is expired: please re-generate credential file using cli tool. ./scripts/gmail_credential.sh")))
    credential))

(defn get-service "
  get gmail api service's connection with application-name (string)
  "
  [^String application-name credential]
  (.. (Gmail$Builder. http-transport json-factory credential)
      (setApplicationName application-name)
      build))

(defmethod ig/init-key ::gmail
  [_ {:keys [env oauth]}]
  (let [credential-file (-> (:gmail-credential-file env) io/file io/as-url)
        tokens-dir (-> (:gmail-tokens-dir env) io/file io/as-url)
        gmail-host-addr (:gmail-host-addr env)
        credential (if oauth
                     (get-new-credential credential-file tokens-dir scopes new-credential-port)
                     (get-credential credential-file tokens-dir scopes))
        service (get-service application-name credential)]
    (timbre/info "load gmail credential ...")
    (timbre/info "credential file: " credential-file)
    (timbre/info "tokens directory: " tokens-dir)
    (timbre/info "gmail host address: " gmail-host-addr)
    (->Boundary {:service service
                 :gmail-host-addr gmail-host-addr})))
