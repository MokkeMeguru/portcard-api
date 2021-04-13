(ns portcard-api.interface.image-db.topic-captures-repository
  (:require [clojure.java.io :as io]
            [portcard-api.interface.image-db.utils :refer [file-extension]])
  (:import javax.imageio.ImageIO))

(defprotocol TopicCaptures
  (get-captures [db])
  (get-capture [db blob])
  (insert-capture [db ^BufferedImage image blob])
  (delete-capture [db blob]))

(defn topic-captures-repository? [inst]
  (satisfies? TopicCaptures inst))

(extend-protocol TopicCaptures
  portcard_api.infrastructure.image_db.image_db.LocalImageBoundary
  (get-captures [{:keys [spec]}]
    {:files (filter #(-> % str file-extension (= ".png")) (file-seq (io/file (:parent spec) "captures")))})
  (insert-capture [{:keys [spec]} image blob]
    (ImageIO/write image "png" (io/file (:parent spec) "captures" blob)))
  (get-capture [{:keys [spec]} blob]
    (let [file (io/file (:parent spec) "captures" blob)]
      (if (.isFile file)
        {:image-stream (io/input-stream file)}
        {})))
  (delete-capture [{:keys [spec]} blob]
    (let [file (io/file (:parent spec) "captures" blob)]
      (if (.isFile file)
        (do (io/delete-file file)
            true)
        false))))
