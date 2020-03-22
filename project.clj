(defproject mhavrlent/clj-openweather-tray "0.0.1"
  :description "System tray app for Windows OS that shows outside temperature in given city using data from openweathermap.org."
  :url "https://github.com/mhavrlent/clj-openweather-tray"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-http "3.10.0"]
                 [org.clojure/data.json "1.0.0"]]
  :main ^:skip-aot clj-openweather-tray.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
