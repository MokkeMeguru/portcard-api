(ns portcard-api.infrastructure.image-db.image-db
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [taoensso.timbre :as timbre]))

(defrecord LocalImageBoundary [spec])

(defn local-image-boundary [env]
  (let [parent-dir "image-db"]
    (->LocalImageBoundary {:parent parent-dir})))

(defmethod ig/init-key ::image-db
  [_ {:keys [env]}]
  (timbre/info "image db with " "local settings")
  (local-image-boundary env))

;; (.isDirectory (io/file "./image-db"))

;; (io/file (.getAbsolutePath (io/file "image-db")) (str (java.util.UUID/randomUUID) ".png"))
