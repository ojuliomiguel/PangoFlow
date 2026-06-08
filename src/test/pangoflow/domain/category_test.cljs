(ns pangoflow.domain.category-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [pangoflow.domain.category :as cats]))

(deftest get-categories-returns-all-six
  (is (= #{:learning :wellness :health :craft :connection :discipline}
         (cats/get-categories))))

(deftest category-label-returns-readable-name
  (is (= "Learning" (cats/category-label :learning))))

(deftest every-category-has-a-label
  (doseq [cat (cats/get-categories)]
    (is (string? (cats/category-label cat)))
    (is (not (str/blank? (cats/category-label cat))))))

(deftest category-color-returns-hex-for-learning
  (is (= "#3b82f6" (cats/category-color :learning))))

(deftest every-category-has-a-hex-color
  (doseq [cat (cats/get-categories)]
    (is (re-matches #"#[0-9a-f]{6}" (cats/category-color cat)))))
