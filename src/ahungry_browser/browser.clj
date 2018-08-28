(ns ahungry-browser.browser)
;; (ns ahungry-browser.browser
;;   (:use ahungry-browser.lib)
;;   (:require [ahungry-browser.lib]))

(import javafx.application.Application)
(import javafx.fxml.FXMLLoader)
(import javafx.scene.Parent)
(import javafx.scene.Scene)
(import javafx.stage.Stage)
(import java.net.URL)
(import java.io.File)

(gen-class
 :extends javafx.application.Application
 :name com.ahungry.Browser)

(def atomic-stage (atom nil))
(defn set-atomic-stage [stage] (swap! atomic-stage (fn [_] stage)))
(defn get-atomic-stage [] @atomic-stage)

;; (def atomic-scenes (atom []))
;; (defn add-scene [scene] (swap! atomic-scenes (fn [_] (conj @atomic-scenes scene))))
;; (defn get-scene [n] (get @atomic-scenes n))
;; (defn get-scenes [] @atomic-scenes)

(defn -start [this stage]
  (let [
        root (FXMLLoader/load (-> "resources/WebUI.fxml" File. .toURI .toURL))
        scene (Scene. root)
        exit (reify javafx.event.EventHandler
               (handle [this event]
                 (println "Goodbye")
                 (javafx.application.Platform/exit)
                 (System/exit 0)))
        ]
    (set-atomic-stage stage)
    (doto stage
      (.setOnCloseRequest exit)
      (.setScene scene)
      (.show))))

;; (defn goto-scene [n]
;;   (run-later
;;    (doto (get-atomic-stage)
;;      (.setScene (get-scene n))
;;      (.show))))

;; (defn new-scene []
;;   (run-later
;;    (let [
;;          root (FXMLLoader/load (-> "resources/WebUI.fxml" File. .toURI .toURL))
;;          scene (Scene. root)
;;          ]
;;      (add-scene scene)
;;      (doto (get-atomic-stage)
;;        (.setScene scene)
;;        (.show)))))
