(ns pangoflow.dashboard-forms-test
  (:require [cljs.test :refer [deftest is]]
            [pangoflow.dashboard-forms :as df]))

(deftest block-rule-description-formats-count-rule
  (is (= "10 pages = 1 block"
         (df/block-rule-description {:type :count :amount 10 :unit :pages}))))

(deftest form-data-from-template-seeds-fields-from-template
  (let [fd (df/form-data-from-template :reading)]
    (is (= :reading (:template-id fd)))
    (is (= "Reading" (:name fd)))
    (is (= "#3b82f6" (:accent-color fd)))
    (is (= :learning (:category fd)))
    (is (false? (:set-target? fd)))))

(deftest form-data-from-template-unknown-id-returns-nil
  (is (nil? (df/form-data-from-template :unknown))))

(deftest overrides-from-form-data-applies-edited-fields
  (let [fd {:name "My Reading"
            :accent-color "#000000"
            :category :wellness
            :set-target? true
            :target-value 2
            :default-session-duration nil}
        o (df/overrides-from-form-data fd :count)]
    (is (= "My Reading" (:name o)))
    (is (= "#000000" (:accent-color o)))
    (is (= :wellness (:category o)))
    (is (= {:value 2 :period :day} (:target o)))))

(deftest overrides-from-form-data-clears-target-when-unchecked
  (let [o (df/overrides-from-form-data
           {:name "Reading" :accent-color "#3b82f6" :category :learning
            :set-target? false :target-value 1}
           :count)]
    (is (nil? (:target o)))))

(deftest overrides-from-form-data-includes-session-duration-for-duration-mode
  (let [o (df/overrides-from-form-data
           {:name "Meditation" :accent-color "#8b5cf6" :category :wellness
            :set-target? false :target-value 1 :default-session-duration 20}
           :duration)]
    (is (= 20 (:default-session-duration o)))))

(deftest create-activity-from-form-returns-valid-activity
  (let [{:keys [activity errors]} (df/create-activity-from-form
                                    :reading
                                    (df/form-data-from-template :reading))]
    (is (nil? errors))
    (is (some? activity))
    (is (= "Reading" (:name activity)))))

(deftest create-activity-from-form-rejects-empty-name
  (let [fd (assoc (df/form-data-from-template :reading) :name "  ")
        {:keys [activity errors]} (df/create-activity-from-form :reading fd)]
    (is (nil? activity))
    (is (seq errors))))
