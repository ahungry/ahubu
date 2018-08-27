(ns ahungry-browser.core
  (:require [ahungry-browser.lib :as l]
            [ahungry-browser.boot :as b])
  (:gen-class))

(defn -main []
  (println "Starting version 0.0.0")
  (l/async-load "http://ahungry.com" (b/boot))
  (l/inject-firebug b/webengine)
  )
