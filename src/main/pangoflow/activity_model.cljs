(ns pangoflow.activity-model
  (:require [clojure.string :as str]
            [pangoflow.categories :as cats]))

(def ^:private valid-tracking-modes #{:completion :count :duration})

(def ^:private valid-target-periods #{:day :week :month})

(def ^:private template-order
  [:reading :meditation :exercise :coding :writing :practice])

(def ^:private templates
  {:reading {:template-id :reading
             :name "Reading"
             :tracking-mode :count
             :block-rule {:type :count :amount 10 :unit :pages}
             :accent-color "#3b82f6"
             :category :learning}
   :meditation {:template-id :meditation
                :name "Meditation"
                :tracking-mode :duration
                :block-rule {:type :duration :amount 15 :unit :minutes}
                :accent-color "#8b5cf6"
                :category :wellness
                :target {:value 1 :period :day}
                :default-session-duration 15}
   :exercise {:template-id :exercise
              :name "Exercise"
              :tracking-mode :duration
              :block-rule {:type :duration :amount 30 :unit :minutes}
              :accent-color "#ef4444"
              :category :health
              :target {:value 1 :period :day}
              :default-session-duration 30}
   :coding {:template-id :coding
            :name "Coding"
            :tracking-mode :duration
            :block-rule {:type :duration :amount 60 :unit :minutes}
            :accent-color "#f59e0b"
            :category :craft
            :default-session-duration 60}
   :writing {:template-id :writing
             :name "Writing"
             :tracking-mode :count
             :block-rule {:type :count :amount 500 :unit :words}
             :accent-color "#10b981"
             :category :craft}
   :practice {:template-id :practice
              :name "Practice"
              :tracking-mode :completion
              :block-rule {:type :completion :amount 1 :unit :session}
              :accent-color "#ec4899"
              :category :craft}})

(defn- hex-color? [color]
  (and (string? color) (re-matches #"#[0-9A-Fa-f]{6}" color)))

(defn- valid-block-rule? [tracking-mode block-rule]
  (and (map? block-rule)
       (= (:type block-rule) tracking-mode)
       (contains? valid-tracking-modes (:type block-rule))))

(defn- valid-target? [target]
  (and (map? target)
       (number? (:value target))
       (pos? (:value target))
       (contains? valid-target-periods (:period target))))

(defn- valid-category? [category]
  (contains? (cats/get-categories) category))

(defn get-templates
  "Seq of template maps (includes :template-id; no activity :id / :created-at)."
  []
  (mapv templates template-order))

(defn get-template
  [template-id]
  (get templates template-id))

(defn make-activity
  [template-id overrides]
  (when-let [template (get-template template-id)]
    (merge (dissoc template :template-id)
           (or overrides {})
           {:id (str (random-uuid))
            :created-at (.toISOString (js/Date.))})))

(defn validate-activity
  [activity]
  (if-not (map? activity)
    ["activity must be a map"]
    (let [errors
          (cond-> []
            (or (not (string? (:name activity)))
              (str/blank? (:name activity)))
          (conj "name must be a non-empty string")

          (not (contains? valid-tracking-modes (:tracking-mode activity)))
          (conj "tracking-mode must be :completion, :count, or :duration")

          (not (valid-block-rule? (:tracking-mode activity) (:block-rule activity)))
          (conj "block-rule must match tracking-mode")

          (not (hex-color? (:accent-color activity)))
          (conj "accent-color must be a valid hex color")

          (and (some? (:category activity))
               (not (valid-category? (:category activity))))
          (conj "category must be a known category keyword")

          (and (some? (:target activity))
               (not (valid-target? (:target activity))))
          (conj "target must have a positive :value and :period of :day, :week, or :month")

          (and (some? (:default-session-duration activity))
               (not= :duration (:tracking-mode activity)))
          (conj "default-session-duration is only valid for :duration tracking mode")

          (and (= :duration (:tracking-mode activity))
               (some? (:default-session-duration activity))
               (or (not (integer? (:default-session-duration activity)))
                   (not (pos? (:default-session-duration activity)))))
          (conj "default-session-duration must be a positive integer"))]
      (when (seq errors) (vec errors)))))

(defn activity-name [activity] (:name activity))
(defn activity-tracking-mode [activity] (:tracking-mode activity))
(defn activity-block-rule [activity] (:block-rule activity))
(defn activity-target [activity] (:target activity))
(defn activity-accent-color [activity] (:accent-color activity))
(defn activity-category [activity] (:category activity))
(defn activity-default-session-duration [activity] (:default-session-duration activity))
