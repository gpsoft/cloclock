(ns cloclock.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]))

(defn tick
  []
  (let [now (js/Date.)]
    (rf/dispatch [:tick now])))

(defonce ticker (js/setInterval tick 500))

(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:time (js/Date.)
     :with-era false}))

(rf/reg-event-db
  :tick
  (fn [db [_ new-time]]
    (assoc db :time new-time)))

(rf/reg-sub
  :time
  (fn [db _] (:time db)))

(rf/reg-sub
  :with-era
  (fn [db _] (:with-era db)))

(defn date-str
  [dt with-era]
  (let [locale (if with-era "ja-JP-u-ca-japanese" "ja-JP")
        opts (conj {:year "numeric"
                    :month "long"
                    :day "numeric"
                    :weekday "short"}
                   (when with-era [:era "long"]))]
    (.toLocaleDateString dt locale (clj->js opts))))

(defn date-section
  []
  [:div.date-section
   (let [dt @(rf/subscribe [:time])
         with-era @(rf/subscribe [:with-era])]
   (date-str dt with-era))])

(defn time-section
  []
  [:div.time-section
   (let [dt @(rf/subscribe [:time])]
     (.toLocaleTimeString dt "ja-JP"))])

(defn ui
  []
  [:div
   [:div.title "Web Clock in ClojureScript"]
   [date-section]
   [time-section]
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
