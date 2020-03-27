(ns clj-openweather-tray.core
  (:gen-class)
  (:import [java.awt SystemTray TrayIcon PopupMenu MenuItem Font Color]
           [java.awt.event ActionListener]
           (javax.swing UIManager)
           (java.awt.image BufferedImage)
           (java.time Instant ZonedDateTime ZoneOffset))
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.edn :as edn]))

(defn create-tray-icon
  "Creates awt TrayIcon with text instead of icon and Exit MenuItem to close the application."
  [text color font font-style font-size icon-size tooltip-text]
  (let [popup (PopupMenu.)
        font (Font. font font-style font-size)
        img (BufferedImage. icon-size icon-size BufferedImage/TYPE_INT_ARGB)
        g2d (.createGraphics img)
        _ (.setFont g2d font)
        _ (.setColor g2d color)
        x (if (< 2 (count text)) -1 1)
        _ (.drawString g2d text x (+ 1 font-size))
        _ (.dispose g2d)
        tray-icon (TrayIcon. img)
        tray (SystemTray/getSystemTray)
        exit-item (MenuItem. "Exit")
        _ (.addActionListener exit-item (reify ActionListener
                                          (actionPerformed [_ _]
                                            (do (.remove tray tray-icon)
                                                (System/exit 0)))))
        _ (.add popup exit-item)
        _ (.setPopupMenu tray-icon popup)
        _ (.setToolTip tray-icon tooltip-text)]
    tray-icon))

(defn remove-all-icons-from-tray
  "Removes all icons from the tray."
  []
  (let [tray (SystemTray/getSystemTray)
        tray-icons (.getTrayIcons tray)]
    (doseq [ti tray-icons]
      (.remove tray ti))))

(defn add-icon-to-tray
  "Adds icon to the tray."
  [icon]
  (let [tray (SystemTray/getSystemTray)]
    (.add tray icon)))

(defn update-tray-icon
  "Updates tray icon."
  [text color font font-style font-size icon-size tooltip-text]
  (remove-all-icons-from-tray)
  (add-icon-to-tray (create-tray-icon text color font font-style font-size icon-size tooltip-text)))

(defn get-weather-data
  "Gets current weather data for given city id using api key from openweathermap.org.
  List of city IDs can be downloaded here http://bulk.openweathermap.org/sample/."
  [url city-id api-key]
  (let [url (format url city-id api-key)]
    (-> (client/get url)
        (:body)
        (json/read-str :key-fn keyword))))

(defn kelvin-to-celsius
  "Converts Kelvin to Celsius"
  [kelvin]
  (- kelvin 273.15))

(defn kelvin-to-fahrenheit
  "Converts Kelvin to Fahrenheit"
  [kelvin]
  (- (/ kelvin 0.556) 459.67))

(defn convert-temperature
  "Converts given temperature in Kelvin to defined scale, which can be C for Celsius or F for Fahrenheit."
  [temp scale]
  (cond
    (= (str/upper-case scale) "C") (kelvin-to-celsius temp)
    (= (str/upper-case scale) "F") (kelvin-to-fahrenheit temp)))

(defn abs
  "Returns absolute value of x."
  [x]
  (if (neg? x) (- x) x))

(defn find-closest
  "Finds closest key in a sorted map sm using provided key k."
  [sm k]
  (let [a (subseq sm <= k)
        ak (if (empty? a) nil (key (last a)))
        b (subseq sm >= k)
        bk (if (empty? b) nil (key (first b)))]
    (if (= ak k)
      k
      (if (= bk k)
        k
        (if (and (some? ak) (some? bk))
          (if (< (abs (- k bk)) (abs (- k ak)))
            bk
            ak)
          (if (some? ak)
            ak
            (if (some? bk)
              bk)))))))

(defn get-color
  "Returns color based on temperature. The higher the temperature, the warmer the color."
  [temp colors]
  (let [sm (into (sorted-map) colors)
        closest-color-key (find-closest sm (Math/round (.doubleValue temp)))
        closest-color-value (get sm closest-color-key)]
    (Color/decode closest-color-value)))

(defn deg-to-cardinal
  "Converts degrees to cardinal point."
  [deg]
  (let [points ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"]
        points-count (count points)
        index (Math/round (.doubleValue (/ deg (/ 360 points-count))))]
    (get points (mod index points-count))))

(defn clouds-percentage-to-text
  "Converts percentage of clouds to text."
  [perc]
  (cond
    (and (>= perc 0) (<= perc 10)) "Clear sky"
    (and (>= perc 11) (<= perc 24)) "Few clouds"
    (and (>= perc 25) (<= perc 50)) "Scattered clouds"
    (and (>= perc 51) (<= perc 84)) "Broken clouds"
    (and (>= perc 85) (<= perc 100)) "Overcast clouds"))

(defn extract-hour-and-minute-from-epoch
  "Converts epoch time to human readable string."
  [epoch]
  (let [instant (Instant/ofEpochSecond epoch)
        zoned-date-time (ZonedDateTime/ofInstant instant (ZoneOffset/UTC))
        hour (.getHour zoned-date-time)
        minute (.getMinute zoned-date-time)]
    (format "%s:%s UTC" hour minute)))

(defn create-tooltip-text
  "Creates text for the tray tooltip that contains additional weather information."
  [wind-speed wind-deg humidity clouds sunrise sunset]
  (format "Wind speed: %s m/s\nWind direction: %s (%s)\nHumidity: %s %%\nSky: %s\nSunrise: %s\nSunset: %s"
          wind-speed
          wind-deg
          (deg-to-cardinal wind-deg)
          humidity
          (clouds-percentage-to-text clouds)
          (extract-hour-and-minute-from-epoch sunrise)
          (extract-hour-and-minute-from-epoch sunset)))

(defn -main
  "Main method. Updates the tray with temperature from openweathermap.org and then sleeps for x seconds
  (see sleep-interval in config file)."
  [& args]
  (when-not (. SystemTray isSupported)
    (throw (Exception. "SystemTray not supported on this platform.")))

  (while true
    (let [config (edn/read-string (slurp "clj-openweather-tray-conf.edn"))
          weather-data (get-weather-data (:url config)
                                         (:city-id config)
                                         (:api-key config))
          temp (-> weather-data (:main) (:temp))
          converted-temp (convert-temperature temp (:scale config))
          color (get-color temp (:colors config))
          wind-speed (-> weather-data (:wind) (:speed))
          wind-deg (-> weather-data (:wind) (:deg))
          humidity (-> weather-data (:main) (:humidity))
          clouds (-> weather-data (:clouds) (:all))
          sunrise (-> weather-data (:sys) (:sunrise))
          sunset (-> weather-data (:sys) (:sunset))]
      (do
        (UIManager/setLookAndFeel (:look-and-feel config))
        (update-tray-icon (str (Math/round (.doubleValue converted-temp)))
                          color
                          (:font config)
                          (:font-style config)
                          (:font-size config)
                          (:icon-size config)
                          (create-tooltip-text wind-speed wind-deg humidity clouds sunrise sunset))
        (Thread/sleep (:sleep-interval config))))))