(ns portcard-api.usecase.post-contact
  (:require [portcard-api.domain.errors :as errors]
            [portcard-api.interface.database.contacts-repository :as contacts-repository]
            [portcard-api.interface.database.user-profiles-contacts-repository :as user-profiles-contacts-repository]
            [portcard-api.interface.database.users-repository :as users-repository]
            [portcard-api.interface.database.utils :as utils]
            [portcard-api.interface.gmail.gmail :as gmail-repository]
            [portcard-api.util :refer [err->> border-error rand-str]]
            [clojure.spec.alpha :as s]))

(defn ->message [display-name subject from-name from body-text]
  (format "%s 様
平素より Portcard をご利用いただきありがとうございます。
%s (%s) 様より以下のメッセージを頂きましたので、ご連絡差し上げました。
--
%s

%s
--

今後とも、本サービスのご利用のほどよろしくお願いいたします。
Portcard (contact: meguru.mokke@gmail.com)

本メールに覚えがない場合は、お手数ですが meguru.mokke@gmail.com までご連絡下さい。
"
          display-name from-name from subject body-text))

(defn ->subject [display-name from-name subject]
  (format "Portcard: %s さん、 %s さんよりメッセージが届きました。(%s)" display-name from-name subject))

(defn ->uname [{:keys [contact] :as m}]
  [(assoc m :uname (:to contact)) nil])

(defn check-user-exist [{:keys [uname db] :as m}]
  (let [[user err] (err->>
                    {:function #(users-repository/get-user db :uname uname)
                     :error-wrapper errors/database-error}
                    border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? user) [nil errors/user-not-found]
      (:is_deleted user) [nil errors/user-is-deleted]
      :else [(-> m (assoc :user user)) nil])))

(defn get-contact-to [{:keys [user db] :as m}]
  (let [user-id (:uid user)
        [contact err] (err->>
                       {:function #(user-profiles-contacts-repository/get-contact db user-id)
                        :error-wrapper errors/database-error}
                       border-error)]
    (cond
      (not (nil? err)) [nil err]
      (empty? (:email contact)) [nil errors/user-contact-not-found]
      :else [(assoc m :user-contact contact) nil])))

(defn ->contact [{:keys [user contact user-contact] :as m}]
  (let [{:keys [from-name from title body-text]} contact
        {:keys [email]} user-contact
        {:keys [display_name]} user
        to email
        uid (java.util.UUID/randomUUID)
        message (->message display_name title from-name from body-text)
        _subject (->subject display_name from-name title)]
    [(assoc m :contact {:uid uid
                        :to to
                        :subject _subject
                        :title title
                        :from-name from-name
                        :from from
                        :message message}) nil]))

(defn post-contact-gmail [{:keys [gmail-service contact] :as m}]
  (let [[result err] (err->>
                      {:function #(gmail-repository/send-email gmail-service contact)
                       :error-wrapper errors/gmail-error}
                      border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else
      (do (println result)
          [m nil]))))

(def contact-min-period (* 1000 60 30))

(defn check-contact-period [{:keys [db contact user] :as m}]
  (let [[result err] (err->>
                      {:function #(contacts-repository/get-latest-contacts-by-user db (:uid user) (:from contact))
                       :error-wrapper errors/database-error}
                      border-error)
        now (utils/sql-to-long (utils/sql-now))]
    (println "checkcontact-period" now result)
    (cond
      (not (nil? err)) [nil err]
      (empty? result) [m nil]
      (> contact-min-period (- now (:created_at result))) [nil errors/invalid-contact-period]
      :else [m nil])))

(defn post-contact-db [{:keys [db contact user] :as m}]
  (let [user-id (:uid user)
        {:keys [uid title from from-name]} contact
        contact {:uid uid
                 :to user-id
                 :subject title
                 :from from
                 :from-name from-name}
        [result err] (err->>
                      {:function #(contacts-repository/create-contact db contact)
                       :error-wrapper errors/database-error}
                      border-error)]
    (cond
      (not (nil? err)) [nil err]
      :else [m nil])))

(defn post-contact [contact db gmail-service]
  (err->>
   {:db db
    :gmail-service gmail-service
    :contact contact}
   ->uname
   check-user-exist
   get-contact-to
   check-contact-period
   ->contact
   post-contact-gmail
   post-contact-db))

;; {
;;   "from-name": "Meguru"
;;   "from": "meguru.mokke@gmail.com",
;;   "to": "Meguru",
;;   "title": "sample message",
;;   "body-text": "sample text body"
;; }
