(ns pangoflow.local-history
  (:require [clojure.walk :as walk]))

(defprotocol IStorageBackend
  (load-activities! [backend])
  (save-activities! [backend data])
  (on-change [backend callback]))

;; Memory backend for tests
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

;; Chrome storage backend for production
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