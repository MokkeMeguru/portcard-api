(ns portcard-api.interface.image-db.icons-repository
  (:require [portcard-api.interface.database.utils :as utils]
            [clojure.java.io :as io]
            [portcard-api.interface.image-db.utils :refer [file-extension]]
            [portcard-api.infrastructure.image-db.image-db]
            [taoensso.timbre :as timbre])
  (:import javax.imageio.ImageIO))

(defprotocol Icons
  (get-icons [db])
  (insert-icon [db ^BufferedImage image blob])
  (get-icon [db blob])
  (delete-icon [db blob]))

(defn icons-repository? [inst]
  (satisfies? Icons inst))

;; (.isFile (io/file "image-db" "icons" "Mnx4I[pCFOoIggj.png"))

(extend-protocol Icons
  portcard_api.infrastructure.image_db.image_db.LocalImageBoundary
  (get-icons [{:keys [spec]}]
    {:files (filter #(-> % str file-extension (= ".png")) (file-seq (io/file (:parent spec) "icons")))})
  (insert-icon [{:keys [spec]} image blob]
    (timbre/info "image-db/query save icon file "  blob)
    (ImageIO/write image "png" (io/file (:parent spec) "icons" blob)))
  (get-icon [{:keys [spec]} blob]
    (timbre/info "image-db/query get file " blob)
    (let [file (io/file (:parent spec) "icons" blob)]
      (if (.isFile file)
        (do
          (timbre/info "query accepted")
          {:image-stream (io/input-stream file)})
        {})))
  (delete-icon [{:keys [spec]} blob]
    (timbre/info "image-db/query delete the file " blob)
    (let [file (io/file (:parent spec) "icons" blob)]
      (if (.isFile file)
        (do (io/delete-file file)
            true)
        false))))

;; (defonce inst (portcard-api.infrastructure.image-db.image-db/->LocalImageBoundary {:parent "image-db"}))

;; (get-icons inst)
