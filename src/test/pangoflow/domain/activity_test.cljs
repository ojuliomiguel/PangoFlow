(ns pangoflow.domain.activity-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [pangoflow.domain.activity :as am]))

(deftest get-templates-returns-six-templates
  (is (= 6 (count (am/get-templates)))))

(deftest get-template-with-valid-id-returns-required-fields
  (let [t (am/get-template :reading)]
    (is (some? t))
    (is (= :reading (:template-id t)))
    (is (= "Reading" (:name t)))
    (is (= :count (:tracking-mode t)))
    (is (map? (:block-rule t)))
    (is (string? (:accent-color t)))))

(deftest get-template-with-invalid-id-returns-nil
  (is (nil? (am/get-template :not-a-template))))

(deftest make-activity-with-valid-template-sets-id-and-created-at
  (let [a (am/make-activity :reading nil)]
    (is (some? a))
    (is (string? (:id a)))
    (is (re-matches #"^[0-9a-f-]{36}$" (:id a)))
    (is (string? (:created-at a)))
    (is (re-find #"^\d{4}-\d{2}-\d{2}T" (:created-at a)))
    (is (nil? (:template-id a)))))

(deftest make-activity-with-overrides-respects-overrides
  (let [a (am/make-activity :reading {:name "Custom Name" :accent-color "#000000"})]
    (is (= "Custom Name" (am/activity-name a)))
    (is (= "#000000" (am/activity-accent-color a)))
    (is (= :count (am/activity-tracking-mode a)))))

(deftest validate-activity-accepts-valid-completion-activity
  (let [a (am/make-activity :practice nil)]
    (is (nil? (am/validate-activity a)))
    (is (= :completion (am/activity-tracking-mode a)))))

(deftest validate-activity-rejects-empty-name
  (let [a (assoc (am/make-activity :reading nil) :name "")]
    (is (seq (am/validate-activity a)))
    (is (some #(str/includes? % "name") (am/validate-activity a)))))

(deftest validate-activity-rejects-invalid-tracking-mode
  (let [a (assoc (am/make-activity :reading nil) :tracking-mode :calendar)]
    (is (seq (am/validate-activity a)))
    (is (some #(str/includes? % "tracking-mode") (am/validate-activity a)))))

(deftest validate-activity-rejects-session-duration-for-non-duration-mode
  (let [a (assoc (am/make-activity :reading nil) :default-session-duration 15)]
    (is (seq (am/validate-activity a)))
    (is (some #(str/includes? % "default-session-duration") (am/validate-activity a)))))

(deftest validate-activity-accepts-session-duration-for-duration-mode
  (let [a (am/make-activity :meditation nil)]
    (is (nil? (am/validate-activity a)))
    (is (= 15 (am/activity-default-session-duration a)))))
