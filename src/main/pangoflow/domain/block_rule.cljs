(ns pangoflow.domain.block-rule)

(defn entry->blocks
  [entry {:keys [type amount]}]
  (case type
    :completion 1
    :count (/ (:value entry) amount)
    :duration (/ (:value entry) amount)
    0))

(defn whole-blocks [blocks]
  (js/Math.floor blocks))

(defn format-blocks [blocks]
  (let [whole (whole-blocks blocks)
        unit (if (= 1 whole) "Block" "Blocks")]
    (if (zero? (- blocks whole))
      (str whole " " unit)
      (str whole " " unit " (" blocks ")"))))