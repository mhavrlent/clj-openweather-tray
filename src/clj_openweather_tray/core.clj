(ns clj-openweather-tray.core
  (:gen-class)
  (:import [java.awt SystemTray TrayIcon PopupMenu MenuItem Font Color]
           [java.awt.event ActionListener]
           (javax.swing SwingUtilities UIManager)
           (java.awt.image BufferedImage))
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [clojure.data.json :as json]))

(defn create-tray-icon
  "Creates awt TrayIcon with text instead of icon and Exit MenuItem to close the application."
  [text color font font-style font-size icon-size]
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
        _ (.setPopupMenu tray-icon popup)]
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
  [text color font font-style font-size icon-size]
  (remove-all-icons-from-tray)
  (add-icon-to-tray (create-tray-icon text color font font-style font-size icon-size)))

(defn get-temperature
  "Gets current temperature for given city id using api key from openweathermap.org.
  List of city IDs can be downloaded here http://bulk.openweathermap.org/sample/."
  [url city-id api-key]
  (let [url (format url city-id api-key)]
    (-> (client/get url)
        (:body)
        (json/read-str :key-fn keyword)
        (:main)
        (:temp))))

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
  (if-let [a (key (first (rsubseq sm <= k)))]
    (if (= a k)
      a
      (if-let [b (key (first (subseq sm >= k)))]
        (if (< (abs (- (Long/valueOf (name k)) (Long/valueOf (name b))))
               (abs (- (Long/valueOf (name k)) (Long/valueOf (name a)))))
          b
          a)))
    (key (first (subseq sm >= k)))))

(defn get-color
  "Returns color based on temperature. The higher the temperature, the warmer the color."
  [temp colors]
  (let [sm (into (sorted-map) colors)
        closest-color-key (find-closest sm (keyword (str (Math/round (.doubleValue temp)))))
        closest-color-value (get sm closest-color-key)]
    (Color/decode closest-color-value)))

(defn -main
  "Main method. Updates the tray with temperature from openweathermap.org and then sleeps for x seconds
  (see sleep-interval in config file)."
  [& args]
  (when-not (. SystemTray isSupported)
    (throw (Exception. "SystemTray not supported on this platform.")))

  (SwingUtilities/invokeLater
    (let [config (json/read-str (slurp "clj-openweather-tray.conf") :key-fn keyword)
          temp (get-temperature (:url config)
                                (:city-id config)
                                (:api-key config))
          converted-temp (convert-temperature temp (:scale config))
          color (get-color temp (:colors config))]
      (UIManager/setLookAndFeel (:look-and-feel config))
      (while true (do
                    (update-tray-icon (str (Math/round (.doubleValue converted-temp)))
                                      color
                                      (:font config)
                                      (:font-style config)
                                      (:font-size config)
                                      (:icon-size config))
                    (Thread/sleep (:sleep-interval config)))))))