(ns portcard-api.cmd.gmail-credential.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [portcard-api.core :as core]))

(def cli-options
  [["-c" "--cron-jobs" "run as cron job (guaranteed outage)" :default false]
   ["-h" "--help" "show help" :default false]])

(defn -main
  [& args]
  (let [{:keys [options summary]}  (parse-opts (rest *command-line-args*) cli-options)
        {:keys [cron-jobs help]} options]
    (cond
      ;; help (println summary)
      cron-jobs (core/start "cmd/gmail_credential/config_cron.edn")
      :else (core/start "cmd/gmail_credential/config.edn"))))
