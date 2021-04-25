(ns portcard-api.infrastructure.env
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]
            [taoensso.timbre :as timbre])
  (:import (com.google.auth.oauth2 GoogleCredentials)))

(defmethod ig/init-key ::env [_ _]
  (timbre/info "loading environment via environ")
  (let [database-url (env :database-url)
        running (env :env)

        ;; firebase
        firebase-credentials-path (env :google-application-credentials)
        firebase-credentials (GoogleCredentials/getApplicationDefault)
        firebase-database (env :firebase-database)
        ;; gmail
        gmail-credential-file (env :gmail-credential-file)
        gmail-tokens-dir (env :gmail-tokens-dir)
        gmail-host-addr (env :gmail-host-addr)
        ;; google cloud storage
        gcs-captures-bucket-name (env :gcs-captures-bucket-name)
        gcs-icons-bucket-name  (env :gcs-icons-bucket-name)]

    (timbre/info "running in " running)
    (timbre/info "database-url" database-url)
    (timbre/info "firebase-secret-json path" firebase-credentials-path)
    (when (.contains ["test" "dev"] running) (timbre/info "orchestra instrument is active") (st/instrument))
    {:database-url database-url
     :running running
     :migration-folder (env :migration-folder)
     :migration-log    (env :migration-log)
     :new-migration (env :new-migration)
     :firebase-credentials firebase-credentials
     :firebase-database firebase-database
     :gmail-credential-file gmail-credential-file
     :gmail-tokens-dir gmail-tokens-dir
     :gmail-host-addr gmail-host-addr
     :gcs-captures-bucket-name gcs-captures-bucket-name
     :gcs-icons-bucket-name  gcs-icons-bucket-name}))
