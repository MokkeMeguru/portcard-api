(ns portcard-api.core
  (:gen-class)
  (:require [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [integrant.repl :as igr]))

(def config-file
  (if-let [config-file (env :config-file)]
    config-file
    "config.edn"))

(defn load-config [config]
  (-> config
      io/resource
      slurp
      ig/read-string
      (doto
       ig/load-namespaces)))

(defn start []
  (igr/set-prep! (constantly (load-config config-file)))
  (igr/prep)
  (igr/init))

(defn stop []
  (igr/halt))

(defn restart []
  (igr/reset-all))

(defn -main
  [& args]
  (timbre/set-level! :info)
  (println "Hello, World!")
  (-> config-file
      load-config
      ig/init))
