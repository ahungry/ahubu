(ns ahungry-browser.core
  (:use ahungry-browser.lib)
  (:require [ahungry-browser.lib :as l]
            [ahungry-browser.boot :as b]
            [clojure.string :as str]
            ;; [ahungry-browser.browser :as browser]
            )
  (:gen-class))

(defn -main []
  (println "Starting version 0.0.0")
  (b/boot)
  (do
    (Thread/sleep 100)
    (l/async-load "http://ahungry.com"))
  ;; (l/inject-firebug b/webengine)
  )
