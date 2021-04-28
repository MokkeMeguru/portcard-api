(ns portcard-api.infrastructure.sql.sql
  (:require [hikari-cp.core :as hikari-cp]
            [integrant.core :as ig]
            [taoensso.timbre :as timbre])
  (:import [javax.sql DataSource]
           [net.ttddyy.dsproxy QueryInfo]
           [net.ttddyy.dsproxy.listener QueryExecutionListener]
           [net.ttddyy.dsproxy.proxy ParameterSetOperation]
           [net.ttddyy.dsproxy.support ProxyDataSource]))

(defrecord Boundary [spec])

;; define logging
(defn- query-parameters [params]
  (->> params (map (memfn getArgs)) (sort-by #(aget % 0)) (mapv #(aget % 1))))

(defn- query-parameter-lists [^QueryInfo query-info]
  (mapv query-parameters (.getParametersList query-info)))

(defn- logged-query [^QueryInfo query-info]
  (let [query  (.getQuery query-info)
        params (query-parameter-lists query-info)]
    (into [query] (if (= (count params) 1) (first params) params))))

(defn- logging-listener []
  (reify QueryExecutionListener
    (beforeQuery [_ _ _])
    (afterQuery [_ exec-info query-infos]
      (let [elapsed (.getElapsedTime exec-info)
            queries (mapv logged-query query-infos)]
        (if (= (count queries) 1)
          (timbre/info "sql/query" {:query (first queries) :elapsed elapsed})
          (timbre/info "sql/batch-query" {:queries queries :elapsed elapsed}))))))

(defn wrap-logger [datasource]
  (doto (ProxyDataSource. datasource)
    (.addListener (logging-listener))))

(defn unwrap-logger [^DataSource datasource]
  (.unwrap datasource DataSource))

(defmethod ig/init-key ::sql
  [_ {:keys [env]}]
  (let [datasource
        (-> {:jdbc-url (:database-url env)
             :maximum-pool-size 2}
            (hikari-cp/make-datasource)
            wrap-logger)]
    (timbre/info "setup connection pool ...")
    (->Boundary {:datasource
                 datasource})))

(defmethod ig/halt-key! ::sql
  [_ boundary]
  (timbre/info "close connection pool ...")
  (-> boundary
      .spec
      :datasource
      unwrap-logger
      hikari-cp/close-datasource))
