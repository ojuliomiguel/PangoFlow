(ns pangoflow.popup
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]))

(defn- dashboard-url []
  (.getURL js/chrome.runtime "dashboard.html"))

(defn open-dashboard! []
  (.create js/chrome.tabs #js {:url (dashboard-url)}))

(defn popup-view []
  [:section.shell.popup-shell
   [:h1 "PangoFlow"]
   [:p.popup-lead
    "Create and review Activities in the Dashboard. Quick capture in this popup comes next."]
   [:button.btn.btn--primary
    {:type "button"
     :on-click open-dashboard!}
    "Open Dashboard"]])

(defn init []
  (when-let [el (.getElementById js/document "app")]
    (rdom/render (rdom/create-root el) [popup-view])))
