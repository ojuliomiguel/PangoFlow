(ns pangoflow.app.dashboard
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [pangoflow.domain.activity :as am]
            [pangoflow.domain.category :as cats]
            [pangoflow.ui.dashboard-forms :as df]
            [pangoflow.history.local-history :as lh]
            [pangoflow.domain.entry :as entry]
            [pangoflow.domain.block-rule :as br]))

(defonce history (r/atom (lh/empty-payload)))
(defonce selected-template-id (r/atom nil))
(defonce form-data (r/atom nil))
(defonce form-errors (r/atom nil))
(defonce creating? (r/atom false))
(defonce expanded-activity-id (r/atom nil))
(defonce entry-value (r/atom nil))
(defonce entry-error (r/atom nil))

(defn- activities [] (lh/get-activities @history))

(defn- today-str []
  (let [d (js/Date.)]
    (str (.getFullYear d)
         "-"
         (.padStart (str (+ 1 (.getMonth d))) 2 "0")
         "-"
         (.padStart (str (.getDate d)) 2 "0"))))

(defn- today-entries-for [activity-id]
  (lh/get-entries-for-activity-on-date @history activity-id (today-str)))

(defn- today-blocks [activity]
  (let [rule (am/activity-block-rule activity)]
    (->> (today-entries-for (:id activity))
         (map #(br/entry->blocks % rule))
         (reduce + 0))))

(defn- select-template! [template-id]
  (reset! selected-template-id template-id)
  (reset! form-data (df/form-data-from-template template-id))
  (reset! form-errors nil))

(defn- start-creating! []
  (reset! creating? true)
  (reset! selected-template-id nil)
  (reset! form-data nil)
  (reset! form-errors nil))

(defn- cancel-creating! []
  (reset! creating? false)
  (reset! selected-template-id nil)
  (reset! form-data nil)
  (reset! form-errors nil))

(defn- save-activity! [activity]
  (let [updated-payload (-> @history
                            (update :activities conj activity)
                            (lh/set-active-activity-id (:id activity)))]
    (-> (lh/save-history! lh/chrome-storage-backend updated-payload)
        (.then (fn []
                 (reset! history updated-payload)
                 (reset! creating? false)
                 (reset! selected-template-id nil)
                 (reset! form-data nil)
                 (reset! form-errors nil))))))

(defn- submit-form! []
  (let [{:keys [activity errors]}
        (df/create-activity-from-form @selected-template-id @form-data)]
    (if errors
      (reset! form-errors errors)
      (save-activity! activity))))

(defn- update-form! [k v]
  (swap! form-data assoc k v))

(defn- add-manual-entry! [activity]
  (let [v @entry-value
        mode (am/activity-tracking-mode activity)
        error (entry/validate-entry-value mode v)]
    (if error
      (reset! entry-error error)
      (let [e (entry/make-entry (:id activity) (today-str) v)
            updated (lh/add-entry @history e)]
        (reset! history updated)
        (reset! entry-value nil)
        (reset! entry-error nil)
        (lh/save-history! lh/chrome-storage-backend updated)))))

(defn- toggle-expanded! [activity-id]
  (reset! expanded-activity-id
          (when (not= activity-id @expanded-activity-id) activity-id))
  (reset! entry-value nil)
  (reset! entry-error nil))

(defn- category-option [[kw label]]
  ^{:key (name kw)} [:option {:value (name kw)} label])

(defn template-card [template]
  (let [tid (:template-id template)
        selected? (= @selected-template-id tid)]
    [:button.template-card
     {:key (name tid)
      :type "button"
      :class (when selected? "template-card--selected")
      :on-click #(select-template! tid)}
     [:span.template-card__swatch {:style {:background-color (:accent-color template)}}]
     [:span.template-card__body
      [:strong.template-card__name (:name template)]
      [:span.template-card__mode (df/tracking-mode-label (:tracking-mode template))]
      [:span.template-card__rule (df/block-rule-description (:block-rule template))]]]))

(defn template-picker []
  [:section.template-picker
   [:h2.template-picker__title
    (if (seq (activities))
      "Choose a template"
      "Create your first Activity")]
   [:div.template-grid
    (for [t (am/get-templates)]
      ^{:key (name (:template-id t))}
      [template-card t])]])

(defn accent-swatch [color]
  (let [current (= (:accent-color @form-data) color)]
    [:button.swatch
     {:key color
      :type "button"
      :class (when current "swatch--selected")
      :style {:background-color color}
      :aria-label (str "Accent color " color)
      :on-click #(update-form! :accent-color color)}]))

(defn activity-form []
  (when-let [template (am/get-template @selected-template-id)]
    (let [fd @form-data
          duration? (= :duration (:tracking-mode template))]
      [:section.activity-form
       [:h2.activity-form__title (str "New " (:name template))]
       (when (seq @form-errors)
         [:ul.form-errors
          (for [[i msg] (map-indexed vector @form-errors)]
            ^{:key i} [:li msg])])
       [:label.form-field
        [:span "Name"]
        [:input {:type "text"
                 :value (:name fd "")
                 :on-change #(update-form! :name (.. % -target -value))}]]
       [:fieldset.form-field
        [:span "Accent Color"]
        [:div.swatch-row
         (for [c df/preset-accent-colors]
           ^{:key c}
           [accent-swatch c])]
        [:input.color-input
         {:type "color"
          :value (:accent-color fd "#3b82f6")
          :on-change #(update-form! :accent-color (.. % -target -value))}]]
       [:label.form-field
        [:span "Category"]
        [:select {:value (if (:category fd) (name (:category fd)) "")
                  :on-change #(update-form!
                               :category
                               (let [v (.. % -target -value)]
                                 (when (seq v) (keyword v))))}
         [:option {:value ""} "None"]
         (map category-option
              (sort-by second
                       (map (fn [c] [c (cats/category-label c)])
                            (cats/get-categories))))]]
       [:label.form-field.form-field--checkbox
        [:input {:type "checkbox"
                 :checked (:set-target? fd)
                 :on-change #(update-form! :set-target? (.. % -target -checked))}]
        [:span "Set a daily target"]]
       (when (:set-target? fd)
         [:label.form-field
          [:span "Target (blocks per day)"]
          [:input {:type "number"
                   :min 1
                   :value (or (:target-value fd) 1)
                   :on-change #(update-form! :target-value
                                             (js/parseInt (.. % -target -value) 10))}]])
       (when duration?
         [:label.form-field
          [:span "Default session duration (minutes)"]
          [:input {:type "number"
                   :min 1
                   :value (or (:default-session-duration fd) 15)
                   :on-change #(update-form! :default-session-duration
                                             (js/parseInt (.. % -target -value) 10))}]])
       [:p.block-rule-note
        [:span "Block Rule: "]
        (df/block-rule-description (:block-rule template))]
       [:div.form-actions
        [:button.btn.btn--primary {:type "button" :on-click submit-form!}
         "Create Activity"]
        (when (seq (activities))
          [:button.btn.btn--ghost {:type "button" :on-click cancel-creating!}
           "Cancel"])]])))

(defn- manual-entry-form [activity]
  (let [mode (am/activity-tracking-mode activity)
        block-rule (am/activity-block-rule activity)
        entries (today-entries-for (:id activity))
        total (today-blocks activity)]
    [:div.manual-entry
     [:div.manual-entry__total
      [:strong "Today: "]
      (br/format-blocks total)]
     (when (seq entries)
       [:ul.manual-entry__list
        (for [e (reverse entries)]
          ^{:key (:id e)}
          [:li (str (:value e) " " (name (:unit block-rule)))])])
     [:div.manual-entry__form
      [:input.manual-entry__input
       {:type "number"
        :min 1
        :placeholder (case mode
                       :completion "1"
                       (str (:amount block-rule)))
        :value (or @entry-value "")
        :on-change #(let [v (js/parseInt (.. % -target -value) 10)]
                      (reset! entry-value (when (pos? v) v))
                      (reset! entry-error nil))}]
      [:button.btn.btn--primary
       {:type "button"
        :on-click #(add-manual-entry! activity)}
       "Add Entry"]]
     (when @entry-error
       [:p.manual-entry__error @entry-error])]))

(defn activity-row [activity]
  (let [cat (am/activity-category activity)
        expanded? (= (:id activity) @expanded-activity-id)]
    [:li.activity-row {:key (:id activity)}
     [:button.activity-row__toggle
      {:type "button"
       :on-click #(toggle-expanded! (:id activity))}
      [:span.activity-row__swatch
       {:style {:background-color (am/activity-accent-color activity)}}]
      [:span.activity-row__info
       [:strong (am/activity-name activity)]
       [:span.activity-row__meta
        (df/tracking-mode-label (am/activity-tracking-mode activity))
        (when cat
          (str " · " (cats/category-label cat)))]]]
     (when expanded?
       [manual-entry-form activity])]))

(defn activity-list []
  [:section.activity-list
   [:h2 "Your Activities"]
   [:ul.activity-list__items
    (for [a (activities)]
      [activity-row a])]])

(defn dashboard-shell []
  [:div.dashboard
   [:header.dashboard__header
    [:h1 "PangoFlow Dashboard"]]
   (when (seq (activities))
     [activity-list])
   (when (or (empty? (activities)) @creating?)
     [template-picker])
   (when @selected-template-id
     [activity-form])
   (when (and (seq (activities)) (not @creating?) (nil? @selected-template-id))
     [:div.dashboard__actions
      [:button.btn.btn--secondary {:type "button" :on-click start-creating!}
       "Create another"]])])

(defonce root (atom nil))

(defn- mount! []
  (when-let [el (.getElementById js/document "app")]
    (reset! root (rdom/create-root el))
    (rdom/render @root [dashboard-shell])))

(defn init []
  (-> (lh/load-history! lh/chrome-storage-backend)
      (.then (fn [loaded]
               (reset! history loaded)
               (mount!)))
      (.catch (fn [_]
                (reset! history (lh/empty-payload))
                (mount!)))))