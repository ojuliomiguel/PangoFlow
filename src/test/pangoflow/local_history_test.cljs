(ns pangoflow.local-history-test
  (:require [cljs.test :refer [deftest is async]]
            [pangoflow.local-history :as lh]))

(deftest load-activities-with-empty-storage-returns-empty-vector
  (async done
    (-> (lh/load-activities! (lh/create-memory-backend))
        (.then (fn [activities]
                 (is (= [] activities))
                 (done))))))

(deftest save-and-load-activities-roundtrip-is-faithful
  (let [backend (lh/create-memory-backend)
        activity {:id "abc" :name "Reading" :tracking-mode :count}]
    (async done
      (-> (lh/save-activities! backend [activity])
          (.then #(lh/load-activities! backend))
          (.then (fn [activities]
                   (is (= [activity] activities))
                   (done)))))))

(deftest save-activities-with-empty-vector-persists-empty-vector
  (let [backend (lh/create-memory-backend)]
    (async done
      (-> (lh/save-activities! backend [])
          (.then #(lh/load-activities! backend))
          (.then (fn [activities]
                   (is (= [] activities))
                   (done)))))))

(deftest on-change-fires-when-storage-mutates
  (let [backend (lh/create-memory-backend)
        called? (atom false)]
    (async done
      (lh/on-change backend (fn [_] (reset! called? true)))
      (-> (lh/save-activities! backend [{:id "x" :name "Test"}])
          (.then #(js/setTimeout (fn []
                                   (is (= true @called?))
                                   (done))
                                 10))))))