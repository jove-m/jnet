(ns engine.player
  (:require
    [engine.prompt-state :as prompt-state]))

(defn new-player
  [{:keys [user identity deck]}]
  {:deck-list (or deck [])
   :deck (or deck [])
   :discard []
   :hand []
   :hand-size 5
   :identity identity
   :play-area []
   :rfg []
   :scored []
   :set-aside []
   :credits 0
   :clicks 0
   :agenda-points 0
   :name (:username user)})

(defn new-corp [opts]
  (merge
    (new-player opts)
    {:prompt-state (prompt-state/make-prompt-state :corp)
     :bad-publicity {:base 0
                     :additional 0}
     :clicks-per-turn 3}))

(defn new-runner [opts]
  (merge
    (new-player opts)
    {:prompt-state (prompt-state/make-prompt-state :runner)
     :brain-damage 0
     :clicks-per-turn 4
     :link 0
     :memory 4
     :tags 0}))

(defn set-player-prompt
  [player props]
  (update player :prompt-state prompt-state/set-prompt props))

(defn clear-player-prompt
  [player]
  (update player :prompt-state prompt-state/clear-prompt))

(defn credits
  [player]
  (:credits player))
