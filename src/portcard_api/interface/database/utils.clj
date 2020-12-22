(ns portcard-api.interface.database.utils
  (:require
   [next.jdbc :as jdbc]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [clojure.string :as string]
   [next.jdbc.sql :as njs]
   [buddy.hashers :as hashers]
   [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
   [next.jdbc.result-set :as rs]
   [camel-snake-kebab.extras :refer [transform-keys]]))

(defn match-regex [regex s]
  (re-matches regex s))

(defn check-whitespace [s]
  (= (count s) (count (string/trim s))))

(defn hash-password [password]
  (hashers/derive password))

(defn check-password
  "check password
  args:
  -  user: a map contains hashed-password (from db)
  -  password: raw password
  returns:
  user or nil
  "
  [user password]
  (if  (hashers/check password (:password user))
    user nil))

(defn check-identity [password pass-list]
  (filter #(hashers/check password (:users/password %)) pass-list))

(defn long-to-sql [long-time]
  (-> long-time
      tc/from-long
      tc/to-sql-time))

(defn sql-to-long [sql-time]
  (-> sql-time
      tc/from-sql-time
      tc/to-long))

(defn sql-now []
  (tc/to-sql-time (t/now)))

(defn transform-keys-to-snake [m]
  (transform-keys #(->snake_case % :separator \-) m))

(defn transform-keys-to-kebab [m]
  (transform-keys #(->kebab-case % :separator \_) m))

(defn run-sql [spec sql-command-list one?]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (if one?
      (jdbc/execute-one! conn sql-command-list)
      (jdbc/execute! conn sql-command-list))))

(def insert-option  {:return-keys true :builder-fn rs/as-unqualified-lower-maps})
(defn insert! [spec table-key m]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (njs/insert! conn table-key m insert-option)))

(defn update! [spec table-key m idm]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (:next.jdbc/update-count (njs/update! conn table-key (assoc m :updated_at (sql-now))  idm))))

(defn delete! [spec table-key idm]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (:next.jdbc/update-count (njs/delete! conn table-key idm))))

(defn find-by-m [spec table-key m]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (njs/find-by-keys conn table-key m {:return-keys true :builder-fn rs/as-unqualified-lower-maps})))

(defn get-by-id [spec table-key k v]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (njs/get-by-id conn table-key v k {:return-keys true :builder-fn rs/as-unqualified-lower-maps})))

;; (defn upsert-builder [table-key m confks update_funcstr]
;;   (let [ks (map name (keys m))
;;         confs (map name confks)
;;         vs (vals m)]
;;     (concat
;;      [(clojure.string/join " "
;;                            ["insert into" (name table-key)
;;                             "("  (clojure.string/join "," ks) ")"
;;                             "values (" (clojure.string/join "," (-> vs count (repeat "?"))) ")"
;;                             "on conflict" "(" (clojure.string/join "," confs) ")"
;;                             "do" "update set"
;;                             update_funcstr])]
;;      vs)))

;; (defn upsert! [spec table-key m confks update_funcstr]
;;   (let [upsert-vec (upsert-builder table-key m confks update_funcstr)]
;;     (with-open [conn (jdbc/get-connection (:datasource spec))]
;;       (:next.jdbc/update-count (jdbc/execute-one! conn upsert-vec)))))

(defn remove-empty [m]
  (into {} (filter (fn [kv] (-> kv second nil? not)) m)))
