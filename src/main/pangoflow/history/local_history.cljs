(ns pangoflow.history.local-history
  (:require [clojure.walk :as walk]))

(defprotocol IStorageBackend
  (load-activities! [backend])
  (save-activities! [backend data])
  (on-change [backend callback]))


(defrecord MemoryStorageBackend [store-atom listeners storage-key]
  IStorageBackend
  (load-activities! [_]
    (js/Promise.resolve (or @store-atom [])))
  (save-activities! [_ data]
    (reset! store-atom data)
    (doseq [cb @listeners]
      (cb data))
    (js/Promise.resolve nil))
  (on-change [_ callback]
    (swap! listeners conj callback)
    #(swap! listeners disj callback)))

(defn create-memory-backend []
  (->MemoryStorageBackend (atom nil) (atom #{}) "pangoflow:v1:activities"))


(deftype ChromeStorageBackend [storage-key]
  IStorageBackend
  (load-activities! [_]
    (js/Promise.
     (fn [resolve _]
       (.get js/chrome.storage.local storage-key
             (fn [result]
               (let [data (js->clj (aget result storage-key))
                     activities (if (sequential? data)
                                  (walk/keywordize-keys data)
                                  [])]
                 (resolve activities)))))))
  (save-activities! [_ data]
    (js/Promise.
     (fn [resolve _]
       (let [stringified (walk/stringify-keys data)]
         (.set js/chrome.storage.local (clj->js {storage-key stringified})
               (fn []
                 (resolve nil)))))))
  (on-change [_ callback]
    (let [listener (fn [changes area-name]
                     (when-let [change (aget changes storage-key)]
                       (let [new-value (js->clj (aget change "newValue"))
                             activities (if (sequential? new-value)
                                          (walk/keywordize-keys new-value)
                                          [])]
                         (callback activities))))]
      (.addListener js/chrome.storage.onChanged listener)
      #(.removeListener js/chrome.storage.onChanged listener))))

(def memory-storage-backend (create-memory-backend))
(def chrome-storage-backend (ChromeStorageBackend. "pangoflow:v1:activities"))


(def ^:private current-version 1)

(defn empty-payload []
  {:version current-version
   :activities []
   :entries []
   :active-activity-id nil})

(defn get-activities [payload]
  (:activities payload []))

(defn get-entries [payload]
  (:entries payload []))

(defn get-active-activity-id [payload]
  (:active-activity-id payload))

(defn set-active-activity-id [payload activity-id]
  (assoc payload :active-activity-id activity-id))

(defn derive-active-activity-id [payload]
  (or (:active-activity-id payload)
      (when-let [first-activity (first (:activities payload))]
        (:id first-activity))))

(defn add-entry [payload entry]
  (update payload :entries conj entry))

(defn get-entries-for-activity [payload activity-id]
  (into [] (filter #(= activity-id (:activity-id %))) (:entries payload)))

(defn get-entries-for-activity-on-date [payload activity-id date]
  (into [] (filter #(and (= activity-id (:activity-id %))
                         (= date (:date %))))
        (:entries payload)))

(defn- migrate-to-v1 [data]
  (if (map? data)
    data
    (assoc (empty-payload) :activities (vec data))))


(defn load-history! [backend]
  (.then (load-activities! backend)
         (fn [raw]
           (migrate-to-v1 raw))))

(defn save-history! [backend payload]
  (save-activities! backend payload))