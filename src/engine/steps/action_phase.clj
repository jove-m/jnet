(ns engine.steps.action-phase
  (:require
   [engine.messages :as msg]
   [engine.pipeline :as pipeline]
   [engine.steps.prompt :as prompt]))

(defn action-active-prompt
  [& _args]
  {:header "Action Phase"
   :text "You have 3 clicks. Choose an action."
   :buttons [{:text "[click] Gain 1[c]." :arg "credit"}]})

(defn action-prompt-clicked
  [_this game player _arg]
  (let [game (-> game
                 (pipeline/complete-current-step)
                 (msg/add-message "{0} gains 1 credit." [(get game player)]))]
    [true (update-in game [player :credits] inc)]))

(defn action-phase [player]
  (prompt/base-prompt
    {:active-condition player
     :active-prompt action-active-prompt
     :on-prompt-clicked action-prompt-clicked}))
