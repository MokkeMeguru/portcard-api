(ns portcard-api.cmd.cloud.core
  (:gen-class)
  (:require [portcard-api.core :as core]))

(defn -main
  [& args]
  (core/start "cmd/cloud/config.edn"))
