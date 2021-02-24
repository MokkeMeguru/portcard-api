(ns portcard-api.util
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as timbre]))

(defn bind-error [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))

(defmacro err->> [val & fns]
  (let [fns (for [f fns] `(bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))


;; (macroexpand-1
;;  '(err->>
;;   {:call "Hello"}
;;   (fn [param]
;;     (if (.startsWith (:call param) "H")
;;       [param nil]
;;       [nil "is not start of H"]))
;;   (fn [param]
;;     (if (.endsWith (:call param) "!")
;;       [param nil]
;;       [nil "is not end of !"]))))


(defn border-error [{:keys [function error-wrapper]}]
  (try (let [result (function)]
         [result nil])
       (catch clojure.lang.ExceptionInfo e
         (timbre/warn (.getMessage e))
         [nil (error-wrapper (str "spec exception: " (.getMessage e)))])
       (catch java.lang.AssertionError e
         (timbre/warn (.getMessage e))
         [nil (error-wrapper (str "spec exception: " (.getMessage e)))])
       (catch Exception e
         (timbre/warn e)
         [nil (error-wrapper (str "unknown exception: " (.getMessage e)))])))

;; usage of border-error
;; (s/def ::pos-int pos-int?)
;; (s/def ::message string?)
;; (s/def ::number int?)
;; (s/fdef sample-fn
;;   :args (s/cat :number ::pos-int :message ::message)
;;   :ret ::number)

;; (defn sample-fn [number message]
;;   {:pre [(s/valid? ::pos-int number)]
;;    :post [(s/valid? ::number %)]}
;;   (print message)
;;   (/ 15 (dec number)))

;; (border-error #(sample-fn 2 "Hello"))
;; (border-error sample-fn 2 "Hello")

(defn remove-empty [m]
  (into {} (filter (fn [kv] (-> kv second nil? not)) m)))

(defn remove-nil [seq]
  (remove nil? seq))

(def chars-list
  (map char (concat (range 48 58) (range 66 92) (range 97 123))))

(defn rand-str [len]
  (apply str (take len (repeatedly #(nth chars-list (rand (count chars-list)))))))
