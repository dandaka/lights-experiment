(ns lights.my-show
  "Set up the fixtures, effects, and cues I actually want to use."
  ;; TODO: Your list of required namespaces will differ from this, depending on
  ;;       what fixtures you actually use, and what effects and cues you create.
  (:require [afterglow.channels :as chan]
            [afterglow.core :as core]
            [afterglow.controllers :as ct]
            [afterglow.transform :as tf]
            [afterglow.effects :as fx]
            [afterglow.effects.color :as color-fx]
            [afterglow.effects.cues :as cues]
            [afterglow.effects.dimmer :refer [dimmer-effect]]
            [afterglow.effects.fun :as fun]
            [afterglow.effects.movement :as move]
            [afterglow.effects.oscillators :as oscillators]
            [afterglow.effects.params :as params]
            [afterglow.effects.show-variable :as var-fx]
            [afterglow.fixtures.blizzard :as blizzard]
            [afterglow.rhythm :as rhythm]
            [afterglow.show :as show]
            [afterglow.show-context :refer :all]
            [com.evocomputing.colors :refer [color-name create-color hue adjust-hue]]
            [taoensso.timbre :as timbre]))

(defonce ^{:doc "Holds my show if it has been created,
  so it can be unregistered if it is being re-created."}
  my-show
  (atom nil))

(defn rgbd-cheap
  "A simple RGB with dimmer and strobe on 1 channel"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/functions :control 4
                0 {:type :dimmer
                  :label "Dimmer from 0 to 189"
                  :range :variable}
                190 {:type :strobe
                  :label "Strobe from 190"})]
   :name "Simple RGB with dimmer"})

(defn rgb-arch
  "A simple RGB light from achitecture"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)]
   :name "Simple RGB"})

(defn rgbd-simple
  "A simple RGBD from London pub"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/functions :dimmer 4)]
    })

(defn rgbw-simple
  "A simple RGBW from London pub"
  []
  {:channels [(chan/color 1 :red)
              (chan/color 2 :green)
              (chan/color 3 :blue)
              (chan/color 4 :white)]
    })

(defn jb-systems-sirius-8ch
  "JB SYSTEMS - Lyre Sirius 60W"
  []
  {:channels [(chan/pan 1)
              (chan/tilt 2)
              (chan/functions :shutter 3 0 "Shutter Closed" 255 "Shutter Open")
              (chan/dimmer 7)
              ]
    :pan-center 86 :pan-half-circle 170 :tilt-center 35 :tilt-half-circle 224
    })


(defn use-my-show
  "Set up the show on the OLA universes it actually needs."
  []
  (set-default-show!
    (swap! my-show (fn [s]
      (when s
        (show/unregister-show s)
        (with-show s (show/stop!)))
      (show/show :universes [1] :description "Accuraten"))))

  (show/patch-fixture! :back-odd-1 (rgbd-cheap) 1 1 :x 1)
  (show/patch-fixture! :back-even-2 (rgbd-cheap) 1 5 :x 2)
  (show/patch-fixture! :back-odd-3 (rgbd-cheap) 1 9 :x 3)
  (show/patch-fixture! :front-1 (rgb-arch) 1 13  :x 3.5)

  ;; Return the show's symbol, rather than the actual map, which gets huge with
  ;; all the expanded, patched fixtures in it.
  '*show*)

(defn use-london-show
  "Show for the London club"
  []
  (set-default-show!
    (swap! my-show (fn [s]
      (when s
        (show/unregister-show s)
        (with-show s (show/stop!)))

      (show/show :universes [1] :description "London"))))

  (show/patch-fixture! :front-1 (rgbd-simple) 1 1   :x 1.6  :y 3.5 :z 2.6)
  (show/patch-fixture! :front-2 (rgbd-simple) 1 17  :x -1.6 :y 3.5 :z 2.6)

  (show/patch-fixture! :scene-side-1 (rgbw-simple) 1 33  :x 0 :y 1.7 :z 1.5)

  (show/patch-fixture! :head-1 (jb-systems-sirius-8ch) 1 49 :x 3.5  :y 7  :z 2.8)
  (show/patch-fixture! :head-2 (jb-systems-sirius-8ch) 1 65 :x 3.5  :y 7  :z 2.8) 
  (show/patch-fixture! :head-3 (jb-systems-sirius-8ch) 1 81 :x 3.5  :y 7  :z 2.8)
  (show/patch-fixture! :head-4 (jb-systems-sirius-8ch) 1 97 :x 3.5  :y 7  :z 2.8)

  (show/patch-fixture! :back-1 (rgbd-cheap) 1 113 :x -1.757 :y 0.325 :z 0.3)
  (show/patch-fixture! :back-2 (rgbd-cheap) 1 117 :x -1.057 :y 0.325 :z 0.3)
  (show/patch-fixture! :back-3 (rgbd-cheap) 1 121 :x -0.39  :y 0.325 :z 0.3)
  (show/patch-fixture! :back-4 (rgbd-cheap) 1 125 :x 0.39   :y 0.325 :z 0.3)
  (show/patch-fixture! :back-5 (rgbd-cheap) 1 129 :x 1.057  :y 0.325 :z 0.3)
  (show/patch-fixture! :back-6 (rgbd-cheap) 1 133 :x 1.757  :y 0.325 :z 0.3)

  ;; Return the show's symbol, rather than the actual map, which gets huge with
  ;; all the expanded, patched fixtures in it.
  '*show*)

(use-my-show)

;(use-london-show)

; Logging level set to :info
(core/init-logging)

(defn global-color-effect
  "Make a color effect which affects all lights in the sample show.
  This became vastly more useful once I implemented dynamic color
  parameters. Can include only a specific set of lights by passing
  them with :lights"
  [color & {:keys [include-color-wheels? lights] :or {lights (show/all-fixtures)}}]
  (try
    (let [[c desc] (cond (= (type color) :com.evocomputing.colors/color)
     [color (color-name color)]
     (and (satisfies? params/IParam color)
          (= (params/result-type color) :com.evocomputing.colors/color))
     [color "variable"]
     :else
     [(create-color color) color])]
      (color-fx/color-effect (str "Color: " desc) c lights :include-color-wheels? include-color-wheels?))
    (catch Exception e
      (throw (Exception. (str "Can't figure out how to create color from " color) e)))))

(defn global-dimmer-effect
  "Return an effect that sets all the dimmers in the sample rig.
  Originally this had to be to a static value, but now that dynamic
  parameters exist, it can vary in response to a MIDI mapped show
  variable, an oscillator, or the location of the fixture. You can
  override the default name by passing in a value with :effect-name"
  [level & {:keys [effect-name]}]
  (dimmer-effect level (show/all-fixtures) :effect-name effect-name))


(defn fiat-lux
  "Start simple with a cool blue color from all the lights."
  []
  (show/add-effect! :color (global-color-effect "slateblue" :include-color-wheels? true))
  (show/add-effect! :dimmers (global-dimmer-effect 255))
)

(core/start-web-server 16000 false)
(show/start!)     ; Start sending its DMX frames.

; Reset dimmers to full brightness
(show/add-effect! :dimmers (global-dimmer-effect 255))

; 2DO rewrite to connect multiple devices
(defn bind-midi
  "Bind MIDI devices"
  [interface]

  (if (= interface "z1")
    (show/add-midi-control-to-var-mapping
      "Traktor Kontrol Z1 Input" 0 4 :knob-1 :max 360)
  )

  (if (= interface "uno")
    (do
      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 0 :audio-drums :min -10 :max 50)

      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 1 :audio-bass :max 50)

      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 2 :audio-solo :max 50)

      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 3 :audio-voice :min -120 :max 50)

      (show/add-midi-control-to-var-mapping
        "USB Uno MIDI Interface" 9 4 :audio-percussion :max 50)
    )
  )

  (if (= interface "automap")
    (do
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 21 :sparkle-chance :min 0 :max 0.5)
      (show/add-midi-control-to-var-mapping
        "Automap MIDI" 10 22 :sparkle-fade :min 1 :max 2000)
    )
  )

  (if (= interface "uno-clock")
    (show/sync-to-external-clock
      (afterglow.midi/sync-to-midi-clock "USB Uno MIDI Interface"))
    )
)

(bind-midi "automap")

(defn set-hue
  "Set main hue"
  [hue]
  (afterglow.show/set-variable! :main-hue hue)
)

(set-hue 0)

; Max lightness in the show
(afterglow.show/set-variable! :max-lightness 50)

; Osc beat ratio, isn't working yet with oscillators, but hope isn't lost
(afterglow.show/set-variable! :osc-beat-ratio 4)

(defn light-sawtooth
  "Change light according to sawtooth osc"
  []
  (let [light-param (params/build-oscillated-param
    (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true) :max :max-lightness)]
    (show/add-effect! :color (global-color-effect
      (params/build-color-param :h :main-hue :s 100 :l light-param)))
  )
)

(defn light-sine
  "Change light according to sine osc"
  []
  (let [light-param (params/build-oscillated-param
    (oscillators/sine-beat :beat-ratio :osc-beat-ratio :down? true :phase 0.5) :max :max-lightness)]
    (show/add-effect! :color (global-color-effect
      (params/build-color-param :h :main-hue :s 100 :l light-param)))
  )
)

(defn light-sawtooth-phase
  "Change light of fixtures with phase shift. WIP."
  []
  (let [phase-gradient (params/build-spatial-param  ; Spread a phase shift across fixtures
      (show/all-fixtures)
      (fn [head] (- (:x head) (:min-x @(:dimensions *show*)))) :max 1)]
    (let [light-param (params/build-oscillated-param
      (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true :phase phase-gradient) :max :max-lightness)]
      (show/add-effect! :color (global-color-effect
        (params/build-color-param :s 100 :l light-param :h :main-hue))
      )
    )
  )
)

(defn light-sawtooth-color
  "Change light according to sawtooth osc & hue phase"
  ([]
    (light-sawtooth-color (show/all-fixtures))
    )
  ([fixtures]
    (let [light-param (params/build-oscillated-param
      (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true) :max :max-lightness)
          hue-param (params/build-oscillated-param
      (oscillators/square-beat :beat-ratio 16 :down? true) :max 90)
    ]
      (show/add-effect! :color (afterglow.effects.color/color-effect
                          "Light sawtooth color" (params/build-color-param :h :main-hue :s 100 :l light-param :adjust-hue hue-param) fixtures))
    ))
)

(defn light-to-param
  "Change light according to dynamic parameter"
  [param]
  (let [light-param (params/build-oscillated-param
    (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true) :max :max-lightness)
        hue-param (params/build-oscillated-param
    (oscillators/square-beat :beat-ratio 16 :down? true) :max 90)
  ]
    (show/add-effect! :color (global-color-effect
      (params/build-color-param :h :main-hue :s 100 :l param)))
  )
)

(defn separate-colors
  "Asign different colors to different fixtures. Experiment."
  []
  (afterglow.effects/scene "Different colors"
    (show/add-effect! :color (afterglow.effects.color/color-effect
      "Plain red" (create-color "red") (show/fixtures-named "back-odd")))
    (show/add-effect! :color (afterglow.effects.color/color-effect
      "Plain Blue" (create-color "blue") (show/fixtures-named "back-even")))
    )
  )

(defn reset-beat
  "Reset beat of the show"
  []
  (afterglow.rhythm/metro-start (:metronome *show*) 1)
)

(fiat-lux)

(defn london-temp
  "Temporary function for testing"
  []
  (show/add-effect! :color (global-color-effect
    (params/build-color-param :h :main-hue :s 100 :l 20)))
  (show/add-effect! :dimmers (global-dimmer-effect 255))
  )

(defn shutter-open
  "Open shutters on heads in London show"
  []
  (show/add-effect! :shutter
    (afterglow.effects.channel/channel-effect "Shutter" 255
      (afterglow.channels/extract-channels
        (show/fixtures-named "head") #(= (:type %) :shutter))))
)

(shutter-open)

(defn calibrate-heads
  "Helper functions to calibrate heads"
  []
  (show/add-effect! :pan-head
    (afterglow.effects.channel/channel-effect
      "Pan" (params/build-variable-param :pan)
      (afterglow.channels/extract-channels
        (show/fixtures-named "head") #(= (:type %) :pan))))
  (show/add-effect! :tilt-head
    (afterglow.effects.channel/channel-effect
      "Tilt" (params/build-variable-param :tilt)
      (afterglow.channels/extract-channels
        (show/fixtures-named "head") #(= (:type %) :tilt))))
  (afterglow.show/set-variable! :pan 0)
  (afterglow.show/set-variable! :tilt 0)
  )

(defn head-direction-z
  "Direct all heads to Z = 1"
  []
  ; (afterglow.effects.movement/direction-effect "Towards z" (params/build-direction-param :x 0 :y 0 :z 1) (show/fixtures-named "head"))

  (show/add-effect! :position
    (move/direction-effect
     "Towards z" (params/build-direction-param :x 0 :y 0 :z 1) (show/fixtures-named "head")))
  )

(defn head-aim-center
  "Direct head to the center of dancefloor"
  []
  (show/add-effect! :position
    (move/aim-effect "Center" (params/build-aim-param :x 0 :y 0 :z 5) (show/fixtures-named "head")))
)

; Magic from Afterglow examples
(defonce ^{:doc "Allows effects to set variables in the running show."}
  var-binder
  (atom nil))
(reset! var-binder (var-fx/create-for-show *show*))

(defn new-cues
  "Create cues."
  []

  (ct/set-cue! (:cue-grid *show*) 0 7
      (cues/cue :sparkle (fn [_] (fun/sparkle (show/all-fixtures)
        :chance :sparkle-chance
        :fade-time :sparkle-fade))
          :held true
          :priority 100))

  (ct/set-cue! (:cue-grid *show*) 0 2
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Blue and red"
        (afterglow.effects.color/color-effect
          "Plain red" (create-color "red") (show/fixtures-named "back-odd"))
        (afterglow.effects.color/color-effect
          "Plain Blue" (create-color "blue") (show/fixtures-named "back-even"))
    ))))

  (ct/set-cue! (:cue-grid *show*) 0 1
    (cues/cue :color  (fn [_] (afterglow.effects/scene
      "Sawtooth All"
      (let [light-param (params/build-oscillated-param
        (oscillators/sawtooth-beat :beat-ratio :osc-beat-ratio :down? true) :max :max-lightness)]
        (global-color-effect (params/build-color-param :h :main-hue :s 100 :l light-param))
      )
    ))))

  (ct/set-cue! (:cue-grid *show*) 0 0
    (cues/cue :color-var (fn [_](afterglow.effects/scene
      "Hue Red"
        (fn [_] (var-fx/variable-effect @var-binder :main-hue 0))
      )
    )))
  (ct/set-cue! (:cue-grid *show*) 1 0
    (cues/cue :color-var (fn [_](afterglow.effects/scene
      "Hue Yellow"
        (fn [_] (var-fx/variable-effect @var-binder :main-hue 60))
      )
    )))
  (ct/set-cue! (:cue-grid *show*) 2 0
    (cues/cue :color-var (fn [_](afterglow.effects/scene
      "Hue Green"
        (fn [_] (var-fx/variable-effect @var-binder :main-hue 120))
      )
    )))
  (ct/set-cue! (:cue-grid *show*) 3 0
    (cues/cue :color-var (fn [_](afterglow.effects/scene
      "Hue Blue"
        (fn [_] (var-fx/variable-effect @var-binder :main-hue 180))
      )
    )))
  (ct/set-cue! (:cue-grid *show*) 4 0
    (cues/cue :color-var (fn [_](afterglow.effects/scene
      "Hue Violet"
        (fn [_] (var-fx/variable-effect @var-binder :main-hue 240))
      )
    )))
  (ct/set-cue! (:cue-grid *show*) 5 0
    (cues/cue :color-var (fn [_](afterglow.effects/scene
      "Hue Purple"
        (fn [_] (var-fx/variable-effect @var-binder :main-hue 300))
      )
    )))
)

(new-cues)