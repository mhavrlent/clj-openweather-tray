(ns clj-openweather-tray.core-test
  (:import [java.awt SystemTray Color])
  (:require [clojure.test :refer :all]
            [clj-openweather-tray.core :refer :all]))

(deftest create-tray-icon-test
  (testing "tray icon"
    (let [tray-icon (create-tray-icon "5" Color/WHITE "Arial" 0 11 32)
          img (.getImage tray-icon)
          img-height (.getHeight img)
          img-width (.getWidth img)
          popup (.getPopupMenu tray-icon)
          item (.getItem popup 0)
          item-label (.getLabel item)]
      (testing "image height"
        (is (= 32 img-height)))
      (testing "image width"
        (is (= 32 img-width)))
      (testing "exit item label"
        (is (= "Exit" item-label))))))

(deftest add-icon-to-tray-test
  (testing "add icon to tray"
    (add-icon-to-tray (create-tray-icon "5" Color/WHITE "Arial" 0 11 32))
    (let [tray (SystemTray/getSystemTray)
          tray-icons (.getTrayIcons tray)]
      (testing "number of icons in tray should be one"
        (is (= 1 (count tray-icons)))))))

(deftest remove-all-icons-from-tray-test
  (testing "remove all icons from tray"
    (add-icon-to-tray (create-tray-icon "5" Color/WHITE "Arial" 0 11 32)))
  (remove-all-icons-from-tray)
  (let [tray (SystemTray/getSystemTray)
        tray-icons (.getTrayIcons tray)]
    (testing "number of icons in tray should be zero"
      (is (= 0 (count tray-icons))))))

(deftest update-tray-icon-test
  (testing "update tray icon"
    (update-tray-icon "5" Color/WHITE "Arial" 0 11 32)
    (let [tray (SystemTray/getSystemTray)
          tray-icons (.getTrayIcons tray)]
      (testing "number of icons in tray should be one"
        (is (= 1 (count tray-icons)))))))

(deftest get-temperature-test
  (testing "get temperature"
    (testing "using sample weather API"
      (is (= 300.15 (get-temperature
                      "https://samples.openweathermap.org/data/2.5/weather?id=%s&appid=%s"
                      "2172797"
                      "b6907d289e10d714a6e88b30761fae22"))))))

(deftest kelvin-to-celsius-test
  (testing "kelvin to celsius conversion"
    (testing "1 kelvin to celsius"
      (is (= -272.15 (kelvin-to-celsius 1))))
    (testing "273.15 kelvin to celsius"
      (is (= 0.0 (kelvin-to-celsius 273.15))))))

(deftest kelvin-to-fahrenheit-test
  (testing "kelvin to fahrenheit conversion"
    (testing "1 kelvin to fahrenheit"
      (is (= -457.8714388489209 (kelvin-to-fahrenheit 1))))
    (testing "273.15 kelvin to fahrenheit"
      (is (= 31.606978417266077 (kelvin-to-fahrenheit 273.15))))))

(deftest convert-temperature-test
  (testing "testing temperature conversion using"
    (testing "celsius scale"
      (is (= -272.15 (convert-temperature 1 "C"))))
    (testing "fahrenheit scale"
      (is (= -457.8714388489209 (convert-temperature 1 "F"))))))

(deftest get-color-test
  (testing "get white color for 273 K"
      (let [color (get-color 273 {:273 "#ffffff"})]
        (do (is (= 255 (.getRed color)))
            (is (= 255 (.getGreen color)))
            (is (= 255 (.getBlue color)))))))