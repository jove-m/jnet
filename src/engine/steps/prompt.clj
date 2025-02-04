(ns engine.steps.prompt
  "Prompt Steps are steps that "
  (:require
   [engine.pipeline :as pipeline]
   [engine.player :as player]
   [engine.steps.step :as step]
   [malli.core :as m]
   [malli.error :as me]
   [malli.util :as mu]))

(def BasePromptSchema
  (mu/merge
    step/BaseStepSchema
    [:map {:closed true}
     [:complete? boolean?]
     [:active-condition
      [:or [:enum :corp :runner] [:=> [:cat :map :map :keyword] :boolean]]]
     [:active-prompt [:=> [:cat :map :map :keyword] :map]]
     [:waiting-prompt [:map [:text :string]]]
     [:on-prompt-clicked [:=> [:cat :map :map [:enum :corp :runner] :string]
                          [:cat :boolean :any]]]]))

(def validate-prompt (m/validator BasePromptSchema))
(def explain-prompt (m/explainer BasePromptSchema))

(defrecord PromptStep
  [complete? on-prompt-clicked continue-step type uuid]
  step/Step
  (continue-step [this game] (continue-step this game))
  (complete? [_] complete?)
  (on-prompt-clicked [this game player arg]
    (on-prompt-clicked this game player arg))
  (validate [this]
    (if (validate-prompt this)
      this
      (let [explained-error (explain-prompt (into {} this))]
        (throw (ex-info (str "Prompt step isn't valid: " (pr-str (me/humanize explained-error)))
                        (select-keys explained-error [:errors])))))))

(defn bind-buttons
  [step prompt]
  (if-let [buttons (:buttons step)]
    (assoc prompt :buttons buttons)
    prompt))

;; active-prompt -> PromptOptsSchema
(defn set-active-prompt
  [game player {:keys [active-prompt] :as this}]
  (->> (active-prompt this game player)
       (bind-buttons this)
       (update game player player/set-player-prompt)))

(defn set-waiting-prompt
  [game player {:keys [waiting-prompt]}]
  (update game player player/set-player-prompt waiting-prompt))

(defn set-prompt
  [{:keys [active-condition] :as this} game]
  (let [[active-player waiting-player]
        (if (active-condition this game :corp)
          [:corp :runner]
          [:runner :corp])]
    (-> game
        (set-active-prompt active-player this)
        (set-waiting-prompt waiting-player this))))

(defn clear-prompt
  [game]
  (-> game
      (update :corp player/clear-player-prompt)
      (update :runner player/clear-player-prompt)))

(defn prompt-continue-step
  [this game]
  (let [completed (step/complete? this)
        game (if completed
               (clear-prompt game)
               (set-prompt this game))]
    [completed game]))

(defn base-prompt
  [{:keys [active-condition active-prompt waiting-text
           on-prompt-clicked]}]
  (->> {:active-condition
        (cond
          (fn? active-condition) active-condition
          (keyword? active-condition) (fn [_this _game player] (= player active-condition)))
        :active-prompt active-prompt
        :waiting-prompt {:text (or waiting-text "Waiting for opponent")}
        :complete? false
        :continue-step prompt-continue-step
        :on-prompt-clicked (or on-prompt-clicked
                               (fn [_this game _player _arg] [false game]))
        :type :step/prompt
        :uuid (java.util.UUID/randomUUID)}
       (map->PromptStep)
       (step/validate)))

(defn handler-active-prompt
  [active-text buttons]
  (fn [_this _game _player]
    {:header "Choices prompt"
     :text (or active-text "Select one")
     :buttons buttons}))

(defn handler-on-prompt-clicked
  [choices]
  (fn [_this game _player arg]
    (if-let [choic (get choices arg)]
      [true (-> game
                (choic)
                (pipeline/complete-current-step))]
      [false game])))

(def HandlerPromptPropsSchema
  [:map {:closed true}
   [:active-condition
    {:optional true}
    [:or [:enum :corp :runner] [:=> [:cat :map :map :keyword] :boolean]]]
   [:active-text {:optional true} :string]
   [:waiting-text {:optional true} :string]
   [:choices [:map-of :string [:=> [:cat :map] :map]]]])

(def validate-handler-props (m/validator HandlerPromptPropsSchema))
(def explain-handler-props (m/explainer HandlerPromptPropsSchema))

(defn handler-prompt
  [props]
  (assert (validate-handler-props props) (me/humanize (explain-handler-props props)))
  (let [{:keys [active-condition active-text waiting-text choices]} props
        buttons (mapv (fn [k] {:text k :arg k}) (keys choices))]
    (base-prompt
      {:active-condition active-condition
       :waiting-text waiting-text
       :active-prompt (handler-active-prompt active-text buttons)
       :on-prompt-clicked (handler-on-prompt-clicked choices)})))
