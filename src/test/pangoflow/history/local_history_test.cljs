(ns pangoflow.history.local-history-test
  (:require [cljs.test :refer [deftest is async]]
            [pangoflow.history.local-history :as lh]))

(defn- sample-activity []
  {:id "abc" :name "Reading" :tracking-mode :count
   :block-rule {:type :count :amount 10 :unit :pages}
   :accent-color "#3b82f6"})

(deftest load-history-with-old-flat-vector-migrates-to-v1
  (let [backend (lh/create-memory-backend)
        old-activity (sample-activity)]
    (async done
      (-> (lh/save-activities! backend [old-activity])
          (.then #(lh/load-history! backend))
          (.then (fn [payload]
                   (is (map? payload))
                   (is (= 1 (:version payload)))
                   (is (vector? (lh/get-activities payload)))
                   (is (= 1 (count (lh/get-activities payload))))
                   (is (= "abc" (:id (first (lh/get-activities payload)))))
                   (done)))))))

(deftest load-history-with-empty-storage-returns-empty-v1-payload
  (async done
    (-> (lh/load-history! (lh/create-memory-backend))
        (.then (fn [payload]
                 (is (map? payload))
                 (is (= 1 (:version payload)))
                 (is (= [] (lh/get-activities payload)))
                 (is (= [] (lh/get-entries payload)))
                 (is (nil? (lh/get-active-activity-id payload)))
                 (done))))))

(deftest save-and-load-history-roundtrip-is-faithful
  (let [backend (lh/create-memory-backend)
        activity (sample-activity)
        payload (-> (lh/empty-payload)
                    (lh/set-active-activity-id "abc"))]
    (async done
      (-> (lh/save-history! backend (assoc payload :activities [activity]))
          (.then #(lh/load-history! backend))
          (.then (fn [loaded]
                   (is (= 1 (:version loaded)))
                   (is (= [activity] (lh/get-activities loaded)))
                   (is (= "abc" (lh/get-active-activity-id loaded)))
                   (is (= [] (lh/get-entries loaded)))
                   (done)))))))

(deftest derive-active-activity-id-with-activities-and-no-active-returns-first
  (let [payload (assoc (lh/empty-payload) :activities [(sample-activity)
                                                        (assoc (sample-activity) :id "def")])]
    (is (= "abc" (lh/derive-active-activity-id payload)))))

(deftest derive-active-activity-id-with-active-set-returns-it
  (let [payload (-> (lh/empty-payload)
                    (assoc :activities [(sample-activity)])
                    (lh/set-active-activity-id "abc"))]
    (is (= "abc" (lh/derive-active-activity-id payload)))))

(deftest derive-active-activity-id-with-no-activities-returns-nil
  (is (nil? (lh/derive-active-activity-id (lh/empty-payload)))))

(deftest set-active-activity-id-updates-payload
  (let [payload (lh/set-active-activity-id (lh/empty-payload) "xyz")]
    (is (= "xyz" (lh/get-active-activity-id payload)))))

(deftest on-change-fires-when-storage-mutates
  (let [backend (lh/create-memory-backend)
        called? (atom false)]
    (async done
      (lh/on-change backend (fn [_] (reset! called? true)))
      (-> (lh/save-history! backend (lh/empty-payload))
          (.then #(js/setTimeout (fn []
                                   (is (= true @called?))
                                   (done))
                                 10))))))

(deftest add-entry-conjions-to-payload
  (let [entry {:id "e1" :activity-id "abc" :date "2026-06-07" :value 1}
        payload (lh/add-entry (lh/empty-payload) entry)]
    (is (= [entry] (lh/get-entries payload)))))

(deftest add-entry-preserves-existing-entries
  (let [e1 {:id "e1" :activity-id "abc" :date "2026-06-07" :value 1}
        e2 {:id "e2" :activity-id "abc" :date "2026-06-07" :value 2}
        payload (-> (lh/empty-payload)
                    (lh/add-entry e1)
                    (lh/add-entry e2))]
    (is (= [e1 e2] (lh/get-entries payload)))))

(deftest get-entries-for-activity-filters-by-activity-id
  (let [e1 {:id "e1" :activity-id "abc" :date "2026-06-07" :value 1}
        e2 {:id "e2" :activity-id "def" :date "2026-06-07" :value 2}
        payload (-> (lh/empty-payload)
                    (lh/add-entry e1)
                    (lh/add-entry e2))]
    (is (= [e1] (lh/get-entries-for-activity payload "abc")))
    (is (= [e2] (lh/get-entries-for-activity payload "def")))))

(deftest get-entries-for-activity-on-date-filters-by-date
  (let [e1 {:id "e1" :activity-id "abc" :date "2026-06-07" :value 1}
        e2 {:id "e2" :activity-id "abc" :date "2026-06-08" :value 2}
        payload (-> (lh/empty-payload)
                    (lh/add-entry e1)
                    (lh/add-entry e2))]
    (is (= [e1] (lh/get-entries-for-activity-on-date payload "abc" "2026-06-07")))
    (is (= [e2] (lh/get-entries-for-activity-on-date payload "abc" "2026-06-08")))
    (is (= [] (lh/get-entries-for-activity-on-date payload "abc" "2026-06-09")))))