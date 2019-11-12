(ns cloclock.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]))

;; Generate tick events
(defn tick
  []
  (let [now (js/Date.)]
    (rf/dispatch [:time-change now])))
(defonce ticker (js/setInterval tick 200))

;; re-frame Events
(declare day-part)
(rf/reg-event-db
  :initialize
  (fn [_ _]
    (let [now (js/Date.)]
      {:time now
       :day-part (day-part now)
       :options {:with-era? true
                 :stop2go? true
                 :background? true}})))

(rf/reg-event-db
  :time-change
  (fn [db [_ new-time]]
    (assoc db :time new-time :day-part (day-part new-time))))

(rf/reg-event-db
  :era-check-change
  (fn [db [_ new-with-era?]]
    (assoc-in db [:options :with-era?] new-with-era?)))

(rf/reg-event-db
  :stop2go-check-change
  (fn [db [_ new-stop2go?]]
    (assoc-in db [:options :stop2go?] new-stop2go?)))

(rf/reg-event-db
  :bg-check-change
  (fn [db [_ new-bg?]]
    (assoc-in db [:options :background?] new-bg?)))

;; re-frame subscriptions
(rf/reg-sub
  :time
  (fn [db _] (:time db)))

(rf/reg-sub
  :day-part
  (fn [db _] (:day-part db)))

(rf/reg-sub
  :options
  (fn [db _] (:options db)))

(rf/reg-sub
  :with-era?
  (fn [db _] (get-in db [:options :with-era?])))

(rf/reg-sub
  :stop2go?
  (fn [db _] (get-in db [:options :stop2go?])))

(rf/reg-sub
  :background?
  (fn [db _] (get-in db [:options :background?])))


;; Utils
(defn day-part
  [dt]
  (let [between (fn [[begin end] v] (and (>= v begin) (< v end)))]
    (condp between (.getHours dt)
      [5 8] :morning
      [8 16] :daytime
      [16 20] :evening
      :night)))

(defn date-str
  [dt with-era?]
  (let [locale (if with-era? "ja-JP-u-ca-japanese" "ja-JP")
        opts (conj {:year "numeric"
                    :month "long"
                    :day "numeric"
                    :weekday "short"}
                   (when with-era? [:era "long"]))]
    (.toLocaleDateString dt locale (clj->js opts))))

(defn ratio->deg
  [rat]
  (* 360 rat))

(defn hms
  [dt]
  [(.getHours dt)
   (.getMinutes dt)
   (.getSeconds dt)
   (.getMilliseconds dt)])

(defn hands-deg
  [dt]
  (let [[h m s] (hms dt)
        sr (/ s 60)
        mr (+ (/ m 60) (/ sr 60))
        hr (+ (/ (mod h 12) 12) (/ mr 12))]
    [(ratio->deg hr)
     (ratio->deg mr)
     (ratio->deg sr)]))

(defn stop?
  [s ms]
  (or (and (zero? s) (< ms 500))
      (>= s 58)))

(defn hands-deg-stop2go
  [dt]
  (let [[h m s ms] (hms dt)
        sr (if (stop? s ms) 0 (+ (/ s 58) (/ ms 1000 60)))
        mr (/ m 60)
        hr (+ (/ (mod h 12) 12) (/ mr 12))]
    [(ratio->deg hr)
     (ratio->deg mr)
     (ratio->deg sr)]))

(defn image-url
  [part]
  (let [kws (case part
              :morning "morning,sunrise,dawn"
              :daytime "daytime,afternoon"
              :evening "evening,sunset,dusk"
              "midnight,sky,star/all")]
    (str "url('https://loremflickr.com/600/400/"
         kws
         "')")) )


;; re-frame views
(defn date
  []
  [:div.date-section
   (let [dt @(rf/subscribe [:time])
         with-era? @(rf/subscribe [:with-era?])]
   (date-str dt with-era?))])

(defn digital-time
  []
  [:div.digital-time-section
   (let [dt @(rf/subscribe [:time])
         opts #js {:hour "2-digit"
                   :minute "2-digit"}]
     (.toLocaleTimeString dt "ja-JP" opts))])

(defn hand
  [src rot]
  [:img.hand {:src src
              :style {:transform (str "rotate(" rot "deg)")}}])

(defn analog-time
  []
  (let [dt @(rf/subscribe [:time])
        stop2go? @(rf/subscribe [:stop2go?])
        [hd md sd] (if stop2go? (hands-deg-stop2go dt) (hands-deg dt))]
    [:div.analog-time-section
     [hand "img/hand_h.png" hd]
     [hand "img/hand_m.png" md]
     [hand "img/hand_s.png" sd]]))

(defn checkbox
  [checked? caption ev]
  [:label.check-label
   [:input {:type "checkbox"
            :value 1
            :checked checked?
            :on-change #(rf/dispatch
                          [ev (not checked?)])}
    ]
   caption])

(defn controls
  []
  (let [{:keys [with-era? stop2go? background?]}
        @(rf/subscribe [:options])]
    [:div.controls
     [checkbox with-era? "和暦で表示" :era-check-change]
     [checkbox stop2go? "Stop2go" :stop2go-check-change]
     [checkbox background? "背景画像" :bg-check-change]]))

(defn display
  []
  (let [background? @(rf/subscribe [:background?])
        part @(rf/subscribe [:day-part])
        bg-image (if background? (image-url part) "none")]
    [:div {:style {:background-image bg-image}
                   :class (str "display " (if background? "bg" "nobg"))}
     [#'date]
     [#'digital-time]
     [#'analog-time]]))

(defn ui
  []
  [:div.content
   [:div.title "Web Clock in ClojureScript"]
   [#'display]
   [#'controls]
   ])


;; app
(defn render
  []
  (reagent/render [ui]
                  (.getElementById js/document "app")))

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])
  (render))

(comment
  (js/alert "Hey")
  (run)
  (rf/dispatch [:time-change (js/Date. "1995-12-17T12:24:00")])
  )
