(ns portcard-api.domain.errors
  (:require [clojure.spec.alpha :as s]))

(defn unknown-error [message]
  {:status 500 :body {:code 777 :message (str message)}})

(def duplicate-user {:status 400 :body {:code 1101 :message "duplicate user_id"}})
(def duplicate-email {:status 400 :body {:code 1102 :message "duplicate email address"}})
(def duplicate-user-name {:status 400 :body {:code 1103 :message "the user name is already used"}})
(def duplicate-account {:status 400 :body {:code 1104 :message "you have already had an account"}})
(def user-creation-error {:status 500 :body {:code 1105 :message "unknown user creation error from db"}})

(def user-not-found {:status 404 :body {:code 1401 :message "user is not found"}})
(def user-is-deleted {:status 404 :body {:code 1402 :message "user is deleted"}})

(def login-failed {:status 500 :body {:code 1501 :message "cannot generate login token"}})
(def invalid-password {:status 400 :body {:code 1502 :message "passward is invalid"}})
(def invalid-authorization {:status 400 :body {:code 1503 :message "login information is invalid"}})
(def invalid-user-operation {:status 400 :body {:code 1504 :message "invalid user's operation"}})

(def picture-not-found {:status 400 :body {:code 1601 :message "picture is not found"}})

(def expired-id-token {:status 400 :body {:code 1701 :message "the firebase token is expired"}})
(def invalid-id-token {:status 400 :body {:code 1702 :message "the firebase token is invalid"}})

(defn database-error [message]
  {:status 500 :body {:code 1802 :message (str "error caused from sql query : " message)}})

(def icon-not-found {:status 400 :body {:code 1801 :message "the icon is not found"}})
(def icon-save-failed {:status 400 :body {:code 1802 :message "the icon image saving failed"}})
