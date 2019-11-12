(ns cloclock.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]))

(defn tick
  []
  (let [now (js/Date.)]
    (rf/dispatch [:time-change now])))

(defonce ticker (js/setInterval tick 200))

(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:time (js/Date.)
     :options {:with-era? false
               :stop2go? false}}))

(rf/reg-event-db
  :time-change
  (fn [db [_ new-time]]
    (assoc db :time new-time)))

(rf/reg-event-db
  :era-check-change
  (fn [db [_ new-with-era?]]
    (assoc-in db [:options :with-era?] new-with-era?)))

(rf/reg-event-db
  :stop2go-check-change
  (fn [db [_ new-stop2go?]]
    (assoc-in db [:options :stop2go?] new-stop2go?)))

(rf/reg-sub
  :time
  (fn [db _] (:time db)))

(rf/reg-sub
  :options
  (fn [db _] (:options db)))

(rf/reg-sub
  :with-era?
  (fn [db _] (get-in db [:options :with-era?])))

(rf/reg-sub
  :stop2go?
  (fn [db _] (get-in db [:options :stop2go?])))

(defn date-str
  [dt with-era?]
  (let [locale (if with-era? "ja-JP-u-ca-japanese" "ja-JP")
        opts (conj {:year "numeric"
                    :month "long"
                    :day "numeric"
                    :weekday "short"}
                   (when with-era? [:era "long"]))]
    (.toLocaleDateString dt locale (clj->js opts))))

(defn date
  []
  [:div.date-section
   (let [dt @(rf/subscribe [:time])
         with-era? @(rf/subscribe [:with-era?])]
   (date-str dt with-era?))])

(defn digital-time
  []
  [:div.digital-time-section
   (let [dt @(rf/subscribe [:time])]
     (.toLocaleTimeString dt "ja-JP"))])

(defn hand
  [src rot]
  [:img.hand {:src src
              :style {:transform (str "rotate(" rot "deg)")}}])

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

(defn hands-deg-stop2go
  [dt]
  (let [[h m s ms] (hms dt)
        sr (if (< s 58) (+ (/ s 58) (/ ms 1000 60)) 0)
        mr (/ m 60)
        hr (+ (/ (mod h 12) 12) (/ mr 12))]
    [(ratio->deg hr)
     (ratio->deg mr)
     (ratio->deg sr)]))

(defn analog-time
  []
  (let [dt @(rf/subscribe [:time])
        stop2go? @(rf/subscribe [:stop2go?])
        [hd md sd] (if stop2go? (hands-deg-stop2go dt) (hands-deg dt))]
    [:div.analog-time-section
     [hand "img/hand_h.png" hd]
     [hand "img/hand_m.png" md]
     [hand "img/hand_s.png" sd]]))

(defn controls
  []
  (let [{:keys [with-era? stop2go?]} @(rf/subscribe [:options])]
    [:div.controls
     [:label.check.with-era
      [:input {:type "checkbox"
               :value with-era?
               :on-change #(rf/dispatch
                             [:era-check-change (not with-era?)])}
       ]
      "和暦で表示"]
     [:label.check.stop2go
      [:input {:type "checkbox"
               :value stop2go?
               :on-change #(rf/dispatch
                             [:stop2go-check-change (not stop2go?)])}
       ]
      "Stop2go"]]))

(defn ui
  []
  [:div
   [:div.title "Web Clock in ClojureScript"]
   [#'date]
   [#'digital-time]
   [#'analog-time]
   [#'controls]
   ])

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
  )
