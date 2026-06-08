(ns pangoflow.domain.entry
  (:require [clojure.string :as str]))

(defn- today-str []
  (let [d (js/Date.)]
    (str (.getFullYear d)
         "-"
         (.padStart (str (+ 1 (.getMonth d))) 2 "0")
         "-"
         (.padStart (str (.getDate d)) 2 "0"))))

(defn make-entry
  ([activity-id value]
   (make-entry activity-id (today-str) value))
  ([activity-id date value]
   {:id (str (random-uuid))
    :activity-id activity-id
    :date (or date (today-str))
    :value value
    :created-at (.toISOString (js/Date.))}))

(defn validate-entry-value
  [tracking-mode value]
  (cond
    (not (number? value))
    (str "value must be a number, got " (type value))

    (not (pos? value))
    "value must be positive"

    (and (= :completion tracking-mode) (not= 1 value))
    "completion entries must have value 1"

    :else
    nil))