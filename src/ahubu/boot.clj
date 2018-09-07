(ns ahubu.boot
  (:use ahubu.lib)
  (:require [ahubu.lib]))

(import javafx.application.Application)
(import javafx.application.Platform)
(import javafx.scene.web.WebView)
(import netscape.javascript.JSObject)
(import javafx.beans.value.ChangeListener)
(import javafx.event.EventHandler)
(import javafx.scene.input.KeyEvent)
(import javafx.concurrent.Worker$State)
(import WebUIController)
(import MyEventDispatcher)

;; https://www.java-forums.org/javafx/93113-custom-javafx-webview-protocol-handler-print.html
;; ;over riding URL handlers
;; (import sun.net.www.protocol.http.Handler)
;; (import sun.net.www.protocol.http.HttpURLConnection)
(import sun.net.www.protocol.https.Handler)
;; (import sun.net.www.protocol.https.HttpsURLConnectionImpl)
(import java.net.URL)
(import java.net.URLConnection)
(import java.net.HttpURLConnection)
(import javax.net.ssl.HttpsURLConnection)
(import java.io.File)
(import java.net.URLStreamHandlerFactory)
(import java.net.URLStreamHandler)

;;launch calls the fxml which in turn loads WebUIController
(defn boot []
  (defonce launch (future (Application/launch com.ahungry.Browser (make-array String 0))))

  (quietly-set-cookies)

  ;; Call this infinitely so it just always keeps setting it
  (future
    (while true
      (do
        ;; (quietly-set-cookies)
        (quietly-set-stream-factory)
        (Thread/sleep 500))))

  ;; @launch
  (do
    (Thread/sleep 1000)
    (new-scene))

  ;; (do
  ;;   ;; Delay to allow JavaFX Toolkit to boot up
  ;;   ;; TODO: Couldn't this be done in a callback or something?
  ;;   (Thread/sleep 100)
  ;;   (new-scene))

  ;; FIXME: Find out why this unbinds seemingly randomly
  )
