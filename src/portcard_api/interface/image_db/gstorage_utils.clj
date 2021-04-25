(ns portcard-api.interface.image-db.gstorage-utils
  (:import [com.google.cloud.storage Blob Blob$BlobSourceOption BlobId BlobInfo Storage$BlobListOption Storage$BlobWriteOption]
           com.google.common.io.ByteStreams
           java.awt.image.BufferedImage
           java.io.FileInputStream
           java.nio.channels.Channels
           javax.imageio.ImageIO)
  (:require [taoensso.timbre :as timbre]))

(defn blob->input-stream
  ([^Blob blob blob-source-options]
   (timbre/info "load image request:" (.getName blob) (.getSelfLink blob))
   (try (Channels/newInputStream
         (.reader blob blob-source-options))
        (catch Exception e
          (timbre/error "input stream transformation failed ... " e))))
  ([^Blob blob]
   (blob->input-stream blob (make-array Blob$BlobSourceOption 0))))
