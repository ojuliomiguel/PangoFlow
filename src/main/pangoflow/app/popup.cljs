(ns pangoflow.app.popup
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [pangoflow.history.local-history :as lh]
            [pangoflow.domain.activity :as am]
            [pangoflow.domain.entry :as entry]
            [pangoflow.domain.block-rule :as br]))

(defonce history (r/atom (lh/empty-payload)))
(defonce quick-add-value (r/atom nil))
(defonce last-quick-add-blocks (r/atom nil))

(defn- dashboard-url []
  (.getURL js/chrome.runtime "dashboard.html"))

(defn open-dashboard! []
  (.create js/chrome.tabs #js {:url (dashboard-url)}))

(defn- active-activity []
  (let [active-id (lh/derive-active-activity-id @history)]
    (some #(when (= (:id active-id) %) %)
          (lh/get-activities @history))))

(defn- switch-active-activity! [activity-id]
  (let [updated (lh/set-active-activity-id @history activity-id)]
    (reset! history updated)
    (reset! last-quick-add-blocks nil)
    (lh/save-history! lh/chrome-storage-backend updated)))

(defn- quick-add-default-value [act]
  (case (am/activity-tracking-mode act)
    :completion 1
    (get-in act [:block-rule :amount] 1)))

(defn- quick-add! [act]
  (let [v (or @quick-add-value (quick-add-default-value act))
        valid (entry/validate-entry-value (am/activity-tracking-mode act) v)]
    (when-not valid
      (let [e (entry/make-entry (:id act) nil v)
            blocks (br/entry->blocks e (am/activity-block-rule act))
            updated (-> @history (lh/add-entry e))]
        (reset! history updated)
        (reset! last-quick-add-blocks blocks)
        (reset! quick-add-value nil)
        (lh/save-history! lh/chrome-storage-backend updated)))))

(defn- today-str []
  (let [d (js/Date.)]
    (str (.getFullYear d)
         "-"
         (.padStart (str (+ 1 (.getMonth d))) 2 "0")
         "-"
         (.padStart (str (.getDate d)) 2 "0"))))

(defn- today-entries [act]
  (lh/get-entries-for-activity-on-date @history (:id act) (today-str)))

(defn- today-daily-total-blocks [act]
  (let [rule (am/activity-block-rule act)]
    (reduce + 0 (map #(br/entry->blocks % rule) (today-entries act)))))

(defn- popup-empty-state []
  [:section.shell.popup-shell
   [:h1 "PangoFlow"]
   [:p.popup-lead
    "Create your first Activity in the Dashboard to start tracking progress."]
   [:button.btn.btn--primary
    {:type "button"
     :on-click open-dashboard!}
    "Open Dashboard"]])

(defn- activity-switcher []
  (let [acts (lh/get-activities @history)
        active-id (:id (active-activity))]
    (when (> (count acts) 1)
      [:select.activity-switcher
       {:value (or active-id "")
        :on-change #(switch-active-activity! (.. % -target -value))}
       (for [a acts]
         ^{:key (:id a)}
         [:option {:value (:id a)} (am/activity-name a)])])))

(defn- quick-add-control [act]
  (let [mode (am/activity-tracking-mode act)]
    [:div.quick-add
     (when (not= :completion mode)
       [:input.quick-add__value
        {:type "number"
         :min 1
         :placeholder (str (get-in act [:block-rule :amount]))
         :value (or @quick-add-value "")
         :on-change #(let [v (js/parseInt (.. % -target -value) 10)]
                       (reset! quick-add-value (when (pos? v) v)))}])
     [:button.btn.btn--primary
      {:type "button"
       :on-click #(quick-add! act)}
      (case mode
        :completion "Complete"
        :count "Add"
        :duration "Log")]]))

(defn- popup-active-state []
  (let [act (active-activity)
        total (today-daily-total-blocks act)
        entries (today-entries act)]
    [:section.shell.popup-shell
     [:h1 "PangoFlow"]
     [activity-switcher]
     [:div.active-activity
      [:span.active-activity__swatch
       {:style {:background-color (am/activity-accent-color act)}}]
      [:span.active-activity__name (am/activity-name act)]
      [:span.active-activity__mode
       (case (am/activity-tracking-mode act)
         :completion "Completion"
         :count "Count"
         :duration "Duration"
         "Progress")]]
     [:div.daily-total
      [:span.daily-total__label "Today"]
      [:span.daily-total__blocks
       (br/format-blocks total)]]
     (when @last-quick-add-blocks
       [:p.quick-add-feedback
        (str "+" (br/format-blocks @last-quick-add-blocks))])
     [quick-add-control act]
     (when (seq entries)
       [:ul.today-entries
        (for [e (reverse entries)]
          ^{:key (:id e)}
          [:li.today-entries__item
           (str (:value e) " "
                (case (am/activity-tracking-mode act)
                  :completion "completion"
                  (:unit (am/activity-block-rule act))))])])
     [:button.btn.btn--secondary
      {:type "button"
       :on-click open-dashboard!}
      "Open Dashboard"]]))

(defn popup-view []
  (if (seq (lh/get-activities @history))
    [popup-active-state]
    [popup-empty-state]))

(defn init []
  (-> (lh/load-history! lh/chrome-storage-backend)
      (.then (fn [loaded]
               (reset! history loaded)))
      (.catch (fn [_]
                (reset! history (lh/empty-payload))))
      (.then (fn []
               (when-let [el (.getElementById js/document "app")]
                 (rdom/render (rdom/create-root el) [popup-view]))))))