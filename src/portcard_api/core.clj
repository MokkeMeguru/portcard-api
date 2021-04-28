(ns portcard-api.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [integrant.repl :as igr]
            [taoensso.timbre :as timbre]))

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

(defn start
  ([]
   (start config-file))
  ([config-file]
   (igr/set-prep! (constantly (load-config config-file)))
   (igr/prep)
   (igr/init)))

(defn stop []
  (igr/halt))

(defn restart []
  (igr/reset-all))

(defn -main
  [& args]
  (timbre/set-level! :info)
  (-> config-file
      load-config
      ig/init))
