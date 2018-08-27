(ns ahungry-browser.browser)
(import javafx.application.Application)
(import javafx.fxml.FXMLLoader)
(import javafx.scene.Parent)
(import javafx.scene.Scene)
(import javafx.stage.Stage)
(import java.net.URL)
(import java.io.File)

(import java.net.HttpURLConnection)
(import javax.net.ssl.HttpsURLConnection)

(gen-class
 :extends javafx.application.Application
 :name com.ahungry.Browser)

(gen-class
 :extends java.net.HttpURLConnection
 :name my.con.Http)

(gen-class
 :extends javax.net.ssl.HttpsURLConnection
 :name my.con.Https)

(import java.net.URLStreamHandler)

(gen-class
 :extends java.net.URLStreamHandler
 :name my.stream.Handler)

(import sun.net.www.protocol.https.HttpsURLConnectionImpl)
(gen-class
 :extends sun.net.www.protocol.https.HttpsURLConnectionImpl
 :name my.con.SunHttps)

;(require '[clojure.java.io :as io])

(defn -start [this stage]
  (let [
         root (FXMLLoader/load (-> "resources/WebUI.fxml" File. .toURI .toURL))
         scene (Scene. root)
         ]
    (.setScene stage scene)
    (.show stage)
    ))
