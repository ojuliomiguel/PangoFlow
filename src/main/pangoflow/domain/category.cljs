(ns pangoflow.domain.category)

(def ^:private categories
  #{:learning :wellness :health :craft :connection :discipline})

(def ^:private labels
  {:learning "Learning"
   :wellness "Wellness"
   :health "Health"
   :craft "Craft"
   :connection "Connection"
   :discipline "Discipline"})

(def ^:private colors
  {:learning "#3b82f6"
   :wellness "#8b5cf6"
   :health "#ef4444"
   :craft "#f59e0b"
   :connection "#14b8a6"
   :discipline "#6366f1"})

(defn get-categories
  []
  categories)

(defn category-label
  [category]
  (get labels category))

(defn category-color
  [category]
  (get colors category))
