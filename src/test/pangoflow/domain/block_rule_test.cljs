(ns pangoflow.domain.block-rule-test
  (:require [cljs.test :refer [deftest is]]
            [pangoflow.domain.block-rule :as br]))

(deftest entry->blocks-completion-returns-one
  (let [entry {:value 1}
        rule {:type :completion :amount 1 :unit :session}]
    (is (= 1 (br/entry->blocks entry rule)))))

(deftest entry->blocks-count-returns-whole-blocks
  (let [entry {:value 50}
        rule {:type :count :amount 10 :unit :pages}]
    (is (= 5 (br/entry->blocks entry rule)))))

(deftest entry->blocks-count-returns-fractional-blocks
  (let [entry {:value 15}
        rule {:type :count :amount 10 :unit :pages}]
    (is (= 1.5 (br/entry->blocks entry rule)))))

(deftest entry->blocks-duration-returns-fractional-blocks
  (let [entry {:value 45}
        rule {:type :duration :amount 60 :unit :minutes}]
    (is (= 0.75 (br/entry->blocks entry rule)))))

(deftest whole-blocks-floors-toward-zero
  (is (= 1 (br/whole-blocks 1.5)))
  (is (= 0 (br/whole-blocks 0.75)))
  (is (= 3 (br/whole-blocks 3.0))))

(deftest format-blocks-shows-whole-number
  (is (= "1 Block" (br/format-blocks 1.0)))
  (is (= "3 Blocks" (br/format-blocks 3.0))))

(deftest format-blocks-shows-fractional-detail
  (let [result (br/format-blocks 1.5)]
    (is (clojure.string/includes? result "1"))
    (is (clojure.string/includes? result "1.5"))))