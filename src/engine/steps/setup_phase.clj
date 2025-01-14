(ns engine.steps.setup-phase
  (:require
   [engine.draw :as draw]
   [engine.steps.step :refer [simple-step]]
   [engine.steps.mulligan-step :as mulligan]
   [engine.steps.phase :as phase]))

(defn setup-begin []
  (simple-step
    (fn [game]
      (-> game
          (assoc-in [:corp :credits] 5)
          (assoc-in [:runner :credits] 5)))))

(defn draw-initial-hands []
  (simple-step
    (fn [game]
      (-> game
          (update-in [:corp :deck] shuffle)
          (update-in [:runner :deck] shuffle)
          (draw/draw :corp 5)
          (draw/draw :runner 5)))))

(defn setup-phase []
  (phase/make-phase
    {:phase :setup
     :steps [(setup-begin)
             (draw-initial-hands)
             (mulligan/mulligan-prompt :corp)
             (mulligan/mulligan-prompt :runner)]}))
