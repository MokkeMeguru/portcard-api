(ns portcard-api.infrastructure.gstorage.image-db
  (:require [clojure.java.io :as io])
  (:import [javax.imageio ImageIO]))

;; (with-open [image-stream (io/input-stream (io/resource "sample.png"))]
;;   (println (type image-stream))
;;   (let [image-buffer (ImageIO/read image-stream)
;;         recon-image-stream ()])


;;   )
(defn hello []
  (println 1))
