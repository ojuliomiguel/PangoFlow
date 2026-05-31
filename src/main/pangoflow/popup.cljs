(ns pangoflow.popup)

(defn set-html! [id html]
  (set! (.-innerHTML (.getElementById js/document id)) html))

(defn init []
  (set-html!
   "app"
   "<section class=\"shell\"><h1>PangoFlow</h1><p>Extension popup shell.</p></section>"))
