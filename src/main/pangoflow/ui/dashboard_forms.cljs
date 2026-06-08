(ns pangoflow.ui.dashboard-forms
  (:require [pangoflow.domain.activity :as am]))

(def preset-accent-colors
  ["#3b82f6" "#8b5cf6" "#ef4444" "#f59e0b" "#10b981" "#ec4899"])

(defn tracking-mode-label
  [mode]
  (case mode
    :completion "Completion"
    :count "Count"
    :duration "Duration"
    (name mode)))

(defn block-rule-description
  [block-rule]
  (let [{:keys [amount unit]} block-rule]
    (str amount " " (name unit) " = 1 block")))

(defn form-data-from-template
  [template-id]
  (when-let [t (am/get-template template-id)]
    {:template-id template-id
     :name (:name t)
     :accent-color (:accent-color t)
     :category (:category t)
     :set-target? (some? (:target t))
     :target-value (or (get-in t [:target :value]) 1)
     :default-session-duration (:default-session-duration t)}))

(defn overrides-from-form-data
  [form-data tracking-mode]
  (cond-> {:name (:name form-data)
           :accent-color (:accent-color form-data)
           :category (:category form-data)}
    (:set-target? form-data)
    (assoc :target {:value (:target-value form-data) :period :day})

    (not (:set-target? form-data))
    (assoc :target nil)

    (= :duration tracking-mode)
    (assoc :default-session-duration (:default-session-duration form-data))

    (not= :duration tracking-mode)
    (assoc :default-session-duration nil)))

(defn create-activity-from-form
  [template-id form-data]
  (if-let [template (am/get-template template-id)]
    (let [overrides (overrides-from-form-data form-data (:tracking-mode template))
          activity (am/make-activity template-id overrides)
          errors (am/validate-activity activity)]
      (if errors
        {:activity nil :errors errors}
        {:activity activity :errors nil}))
    {:activity nil :errors ["unknown template"]}))
