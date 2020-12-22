(ns portcard-api.util
  (:require [clojure.spec.alpha :as s]))

(defn bind-error [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))

(defmacro err->> [val & fns]
  (let [fns (for [f fns] `(bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))


;; (err->>
;;  {:call "Hello"}
;;  (fn [param]
;;    (if (.startsWith (:call param) "H")
;;      [param nil]
;;      [nil "is not start of H"]))
;;  (fn [param]
;;    (if (.endsWith (:call param) "!")
;;      [param nil]
;;      [nil "is not end of !"])))
