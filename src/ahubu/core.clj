(ns ahubu.core
  (:use ahubu.lib)
  (:require [ahubu.lib :as l]
            [ahubu.boot :as b]
            [clojure.string :as str]
            ;; [ahubu.browser :as browser]
            )
  (:gen-class))

(defn -main []
  (println "Starting version 0.0.0")
  (b/boot)
  (do
    (Thread/sleep 100)
    (l/async-load (l/get-default-url))
    ;; (l/async-load "http://ahungry.com")
    )
  ;; (l/inject-firebug b/webengine)
  )
