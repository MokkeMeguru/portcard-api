(ns portcard-api.infrastructure.router.sample
  (:require [reitit.ring.middleware.multipart :as multipart]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [clojure.walk :as w]))

(defn sample-router [env]
  ["/samples"
   ["/ping"
    {:swagger {:tags ["samples"]}
     :post {:summary "ping"
            :responses {200 {:body {:result string?}}}
            :handler (fn [& args]
                       {:status 200
                        :body {:result "pong"}})}}]
   ["/secure-ping"
    {:swagger {:tags ["samples"]}
     :post {:summary "ping"
            :swagger {:security [{:Bearer []}]}
            :responses {200 {:body {:result string?
                                    :bearer string?}}}
            :handler (fn [{:keys [headers]}]
                       (let [bearer (-> headers w/keywordize-keys :authorization)]
                         {:status 200
                          :body {:result "pong"
                                 :bearer bearer}}))}}]

   ["/files"
    {:swagger {:tags ["samples"]}}

    ["/upload"
     {:post {:summary "upload a file"
             :parameters {:multipart
                          {:file multipart/temp-file-part
                           :title string?}}
             :responses {200 {:body {:title string? :file-id string?}}}
             :handler (fn [{{{:keys [file title]} :multipart} :parameters}]
                        (let [fin  (:tempfile file)
                              fout-name (.toString (java.util.UUID/randomUUID))
                              fout (io/file
                                    (str "image-db/" fout-name))]
                          (timbre/info "save title: " title "into: " fout-name)
                          (io/copy fin fout)
                          {:status 200
                           :body {:title title
                                  :file-id fout-name}}))}}]

    ["/download"
     {:get {:summary "downloads a file"
            :swagger {:produces ["image/png"]}
            :handler (fn [_]
                       {:status 200
                        :headers {"Content-Type" "image/png"}
                        :body
                        (io/input-stream (java.net.URL. "https://avatars0.githubusercontent.com/u/30849444?s=460&u=75bde9345fbaf950cceec1d8fc4dc68eff83507a&v=4"))

                        ;;(io/input-stream (io/file "resources/icon.png"))
                        })}}]]

   ["/math"
    {:swagger {:tags ["samples"]}}

    ["/plus"
     {:get {:summary "plus with spec query parameters"
            :parameters {:query {:x int?, :y int?}}
            :responses {200 {:body {:total int?}}}
            :handler (fn [{{{:keys [x y]} :query} :parameters}]
                       {:status 200
                        :body {:total (+ x y)}})}
      :post {:summary "plus with spec body parameters"
             :parameters {:body {:x int?, :y int?}}
             :responses {200 {:body {:total int?}}}
             :handler (fn [{{{:keys [x y]} :body} :parameters}]
                        {:status 200
                         :body {:total (+ x y)}})}}]]])

;; (.isFile (io/file "resources/icon.png"))

;; (.isFile (io/file (.getAbsolutePath (io/file "resources/icon.png"))))
;; (.isDirectory (io/file "resources"))
;; (seq (.list (io/file "resources")))
