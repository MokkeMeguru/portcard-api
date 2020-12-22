(ns portcard-api.usecase.check-id-token
  (:require [portcard-api.interface.firebase.auth :refer [safe-decode-token]]))

(defn check-id-token [m]
  (let [{:keys [result user-id cause]} (safe-decode-token (:id-token m))]
    (if (= :success result)
      [(assoc m :user-id user-id) nil]
      [nil cause])))
