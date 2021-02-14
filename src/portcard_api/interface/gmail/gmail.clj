(ns portcard-api.interface.gmail.gmail
  (:require [portcard-api.infrastructure.gmail.gmail :as  gmail-infrastructure]
            [clojure.java.io :as io]
            [portcard-api.interface.gmail.utils :as gmail-utils]
            [portcard-api.domain.gmail :as gmail-model]
            [clojure.spec.alpha :as s]))


;; define protocol


(defprotocol Gmail
  (send-email [service message]))

(extend-protocol Gmail
  portcard_api.infrastructure.gmail.gmail.Boundary
  (send-email [{:keys [spec]} message]
    {:pre [(s/valid? ::gmail-model/mail-message message)]
     :post [(s/valid? ::gmail-model/gmail-response %)]}
    (let [{:keys [gmail-host-addr service]} spec
          message (gmail-utils/create-message (assoc message :from gmail-host-addr))
          encoded-message (gmail-utils/create-message-with-email message)]
      (gmail-utils/send-message service "me" encoded-message))))

;; (send-email inst {})

;; (def inst
;;   (portcard-api.infrastructure.gmail.gmail/->Boundary
;;    {:service (gmail-infrastructure/get-service
;;               gmail-infrastructure/application-name
;;               (gmail-infrastructure/get-credential
;;                "resources/credentials.json"
;;                "resources/tokens"
;;                gmail-infrastructure/scopes))
;;     :gmail-host-addr "portcard.contact@gmail.com"}))

;; (-> inst
;;     .spec
;;     :service)

;; (def response (send-email inst
;;                           {:to "meguru.mokke@gmail.com"
;;                            :subject "test message"
;;                            :message "hello"}))

;; (s/valid? ::gmail-model/gmail-response  response)
