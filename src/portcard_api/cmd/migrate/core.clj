(ns portcard-api.cmd.migrate.core
  (:gen-class)
  (:require [portcard-api.core :as core]))

(defn -main
  [& args]
  (core/start "cmd/migrate/config.edn"))
