(ns pangoflow.domain.entry-test
  (:require [cljs.test :refer [deftest is]]
            [pangoflow.domain.entry :as entry]))

(deftest make-entry-returns-map-with-required-keys
  (let [e (entry/make-entry "act-1" "2026-06-07" 1)]
    (is (map? e))
    (is (string? (:id e)))
    (is (= "act-1" (:activity-id e)))
    (is (= "2026-06-07" (:date e)))
    (is (= 1 (:value e)))
    (is (string? (:created-at e)))))

(deftest make-entry-defaults-date-to-today
  (let [e (entry/make-entry "act-1" nil 1)]
    (is (string? (:date e)))
    (is (not (clojure.string/blank? (:date e))))))

(deftest validate-entry-value-accepts-positive-number-for-count
  (is (nil? (entry/validate-entry-value :count 10))))

(deftest validate-entry-value-accepts-positive-number-for-duration
  (is (nil? (entry/validate-entry-value :duration 25))))

(deftest validate-entry-value-accepts-one-for-completion
  (is (nil? (entry/validate-entry-value :completion 1))))

(deftest validate-entry-value-rejects-zero
  (is (string? (entry/validate-entry-value :count 0))))

(deftest validate-entry-value-rejects-negative
  (is (string? (entry/validate-entry-value :duration -5))))

(deftest validate-entry-value-rejects-non-number
  (is (string? (entry/validate-entry-value :count "10"))))

(deftest validate-entry-value-rejects-completion-value-not-one
  (is (string? (entry/validate-entry-value :completion 2))))