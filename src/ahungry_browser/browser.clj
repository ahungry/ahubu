(ns ahungry-browser.browser)

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
    (doto stage
      (.setOnCloseRequest exit)
      (.setScene scene)
      (.show))))
