(ns portcard-api.interface.image-processor.core
  (:import [java.awt.image AffineTransformOp BufferedImage]
           [java.io ByteArrayOutputStream FileInputStream File]
           java.awt.geom.AffineTransform
           javax.imageio.ImageIO
           java.net.URLEncoder)
  (:require [clojure.java.io :as io]))

(defn scale
  [image ratio width height]
  (let [scale (AffineTransform/getScaleInstance (double ratio) (double ratio))
        transform-op (AffineTransformOp.
                      scale AffineTransformOp/TYPE_BILINEAR)]
    (.filter transform-op image (BufferedImage. width height (.getType image)))))

(defn scale-image
  [^BufferedImage image thumb-size]
  (let [width (-> image .getWidth)
        height (-> image .getHeight)
        ratio (/ (double thumb-size) height)]
    (scale image ratio thumb-size thumb-size)))

(defn center-crop-image
  [^BufferedImage image]
  (let [width (-> image .getWidth)
        height (-> image .getHeight)
        min-size (min width height)
        x (/ (- width  min-size) 2)
        y (/ (- height min-size) 2)]
    (-> image
        (.getSubimage x y min-size min-size))))

(defn climb-image [^BufferedImage image ^Integer max-size]
  (let [width (-> image .getWidth)
        height (-> image .getHeight)
        scale-rate (/ (double max-size)  (max width height))]
    (if (< scale-rate 1.0)
      (scale image scale-rate (int (* scale-rate width)) (int (* scale-rate height)))
      image)))

(defn ->icon
  [^java.io.InputStream source ^Integer icon-size]
  (-> source
      ImageIO/read
      center-crop-image
      (scale-image icon-size)))

(defn ->climb-image
  [^java.io.InputStream source ^Integer max-size]
  (let [res (-> source
                ImageIO/read
                (climb-image max-size))]
    res))

;; playground
;; (def sample-image (io/as-file (io/resource "ErZ-nluUUAEd5t3.jpeg")))

;; (let [origin  (ImageIO/read sample-image)
;;       image (->climb-image sample-image 512)]
;;   (println "origin" (.getHeight origin) (.getWidth origin))
;;   (println "target" (.getHeight image) (.getWidth image))
;;   (let [fout (io/file "sample2.png")]
;;     (ImageIO/write image "png" fout)))
