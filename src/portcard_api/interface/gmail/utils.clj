(ns portcard-api.interface.gmail.utils "
  - ref:  https://github.com/MokkeMeguru/gmail-clojure
  "

    (:import
     [com.google.api.services.gmail GmailScopes]
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

     [java.util Properties]
     [javax.mail.internet MimeMessage]
     [javax.mail Session]
     [javax.mail.internet InternetAddress]
     [javax.mail Message$RecipientType]
     [org.apache.commons.codec.binary Base64]
     [com.google.api.services.gmail.model Message]
     [com.google.api.client.auth.oauth2 Credential]
     [com.google.api.services.gmail.model Label]))

(def charset "utf-8")
(def encode "base64")

(defn create-message "
  convert message map into mime message
  "
  [{:keys [to from cc subject message]}]
  (let [props (Properties.)
        session (Session/getDefaultInstance props nil)
        email (MimeMessage. session)]
    (doto
     email
      (.setFrom (InternetAddress. from))
      (.addRecipient Message$RecipientType/TO (InternetAddress. to))
      (.addRecipient Message$RecipientType/CC (InternetAddress. cc))
      (.setSubject subject charset)
      (.setText message charset))))

(defn create-message-with-email "
  encode email message into gmail api's code
  "
  [email-content]
  (let [binary-content (with-open [xout (java.io.ByteArrayOutputStream.)]
                         (.writeTo email-content xout)
                         (.toByteArray xout))
        encoded-content (Base64/encodeBase64URLSafeString binary-content)
        message (Message.)]
    (doto
     message
      (.setRaw encoded-content))))

(defn send-message "
  send message using google api
  ```clojure
  (let [credential (get-credential credential-file tokens-dir scopes)
        service (get-service application-name credential)
        user-id \"me\"
        mime-message (create-message message)
        gmail-message (create-message-with-email mime-message)]
    (send-message service user-id gmail-message))
  ```
  "
  [service user-id message]
  (let [response (-> service
                     .users
                     .messages
                     (.send user-id message)
                     .execute)
        response-map  (into {} response)]
    {:id (get response-map "id")
     :label-ids (-> (get response-map "labelIds") vec)
     :thread-id (get response-map "threadId")}))
