(ns portcard-api.infrastructure.sql.migrate
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [ragtime.jdbc :as rjdbc]
            [clj-time.coerce :as tc]
            [ragtime.repl :as ragr]
            [clj-time.core :as time]
            [integrant.core :as ig]
            [clojure.string :as string]))

(def migration-option
  {:encoding "UTF-8"
   :append true})

(defn init-file! [fname]
  (let [{:keys [encoding append]} migration-option]
    (when-not (.exists (io/as-file fname))
      (timbre/info "first logging ...")
      (spit fname "first log\n" :encoding encoding :append append))))

(defn migrate-up! [config migration-log encoding append]
  (timbre/info "migration start!")
  (ragr/migrate config)
  (spit migration-log (str "migrated! at " (tc/to-string (time/now)) "\n")
        :encoding encoding :append append))

(defn migrate! [command database-url migration-folder migration-log]
  (let [{:keys [encoding append]} migration-option
        config {:datastore (rjdbc/sql-database {:connection-uri database-url})
                :migrations (rjdbc/load-resources migration-folder)}]
    (condp = command
      :up (migrate-up! config migration-log encoding append)
      nil)))

(defn rollback! []
  (let [config {:datastore (rjdbc/sql-database {:connection-uri (:database-url environ.core/env)})
                :migrations (rjdbc/load-resources (:migration-folder environ.core/env))}]
    (ragr/rollback config)))

;; (rollback!)
(defmethod ig/init-key ::migrate
  [_ {:keys [env]}]
  (let [{:keys [database-url
                running
                migration-folder
                migration-log
                new-migration]} env
        log (do (init-file! migration-log)
                (string/split-lines (slurp migration-log)))]
    (timbre/info "load migration file: " migration-log)
    (timbre/info "new migration exists?" new-migration)
    (if new-migration
      (migrate! :up database-url migration-folder migration-log)
      (timbre/info "keep database as is"))
    env))
