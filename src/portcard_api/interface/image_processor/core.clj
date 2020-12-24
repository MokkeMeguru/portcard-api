(ns portcard-api.interface.image-processor.core
  (:import [java.awt.image AffineTransformOp BufferedImage]
           [java.io ByteArrayOutputStream FileInputStream File]
           java.awt.geom.AffineTransform
           javax.imageio.ImageIO
           java.net.URLEncoder)
  (:require [clojure.java.io :as io]))

(defn scale [image ratio width height]
  (let [scale (AffineTransform/getScaleInstance (double ratio) (double ratio))
        transform-op (AffineTransformOp.
                      scale AffineTransformOp/TYPE_BILINEAR)]
    (.filter transform-op image (BufferedImage. width height (.getType image)))))

(defn scale-image [^BufferedImage image thumb-size]
  (let [width (-> image .getWidth)
        height (-> image .getHeight)
        ratio (/ (double thumb-size) height)]
    (scale image ratio thumb-size thumb-size)))

(defn center-crop-image [^BufferedImage image]
  (let [width (-> image .getWidth)
        height (-> image .getHeight)
        min-size (min width height)
        x (/ (- width  min-size) 2)
        y (/ (- height min-size) 2)]
    (-> image
        (.getSubimage x y min-size min-size))))

(defn ->icon [^java.io.File source ^Integer icon-size]
  (-> source
      ImageIO/read
      center-crop-image
      (scale-image icon-size)))
