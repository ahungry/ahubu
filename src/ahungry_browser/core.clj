(ns ahungry-browser.core
  (:require [ahungry-browser.lib :as l]
            [ahungry-browser.boot :as b]
            ;; [ahungry-browser.browser :as browser]
            )
  (:gen-class))

(defn -main []
  (println "Starting version 0.0.0")
  (b/boot)
  ;; (l/async-load "http://ahungry.com")
  ;; (l/async-load "http://ahungry.com" (b/boot))
  ;; (l/inject-firebug b/webengine)
  )
