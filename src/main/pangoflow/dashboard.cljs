(ns pangoflow.dashboard)

(defn set-html! [id html]
  (set! (.-innerHTML (.getElementById js/document id)) html))

(defn init []
  (set-html!
   "app"
   "<section class=\"shell\"><h1>PangoFlow Dashboard</h1><p>Dashboard shell.</p></section>"))
