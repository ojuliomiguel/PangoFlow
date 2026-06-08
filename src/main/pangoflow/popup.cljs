(ns pangoflow.popup
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [pangoflow.local-history :as lh]
            [pangoflow.activity-model :as am]))

(defonce history (r/atom (lh/empty-payload)))

(defn- dashboard-url []
  (.getURL js/chrome.runtime "dashboard.html"))

(defn open-dashboard! []
  (.create js/chrome.tabs #js {:url (dashboard-url)}))

(defn- active-activity []
  (let [active-id (lh/derive-active-activity-id @history)]
    (some #(when (= (:id %) active-id) %)
          (lh/get-activities @history))))

(defn- switch-active-activity! [activity-id]
  (let [updated (lh/set-active-activity-id @history activity-id)]
    (reset! history updated)
    (lh/save-history! lh/chrome-storage-backend updated)))

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

(defn- popup-active-state []
  (let [act (active-activity)]
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
     [:p.popup-lead
      "Quick capture and Focus Session coming soon."]
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