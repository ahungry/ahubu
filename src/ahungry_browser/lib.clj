(ns ahungry-browser.lib
  (:require [clojure.string :as str]))

(import MyEventDispatcher)
(import WebUIController)
(import java.io.File)
(import java.io.File)
(import java.net.HttpURLConnection)
(import java.net.URL)
(import java.net.URL)
(import java.net.URLConnection)
(import java.net.URLStreamHandler)
(import java.net.URLStreamHandlerFactory)
(import javafx.application.Application)
(import javafx.application.Platform)
(import javafx.beans.value.ChangeListener)
(import javafx.concurrent.Worker$State)
(import javafx.event.EventHandler)
(import javafx.fxml.FXMLLoader)
(import javafx.scene.Parent)
(import javafx.scene.Scene)
(import javafx.scene.control.Label)
(import javafx.scene.input.KeyEvent)
(import javafx.scene.web.WebView)
(import javafx.stage.Stage)
(import javax.net.ssl.HttpsURLConnection)
(import netscape.javascript.JSObject)
(import sun.net.www.protocol.https.Handler)

(gen-class
 :extends javafx.application.Application
 :name com.ahungry.Browser)

(declare keys-g-map)
(declare keys-default)
(declare bind-keys)
(declare new-scene)
(declare goto-scene)
(declare hide-buffers)
(declare show-buffers)
(declare filter-buffers)

(def atomic-stage (atom nil))
(defn set-atomic-stage [stage] (swap! atomic-stage (fn [_] stage)))
(defn get-atomic-stage [] @atomic-stage)

;; Each scene is basically a tab
(def atomic-scenes (atom []))
(defn add-scene [scene] (swap! atomic-scenes (fn [_] (conj @atomic-scenes scene))))
(defn get-scene [n] (get @atomic-scenes n))
(defn get-scenes [] @atomic-scenes)

(def atomic-scene-id (atom 0))
(defn set-scene-id [n] (swap! atomic-scene-id (fn [_] n)))
(defn get-scene-id [] @atomic-scene-id)

(def atomic-new-tab (atom false))
(defn set-new-tab [b] (swap! atomic-new-tab (fn [_] b)))
(defn get-new-tab? [] @atomic-new-tab)

(def atomic-default-url (atom "http://ahungry.com"))
(defn set-default-url [s] (swap! atomic-default-url (fn [_] s)))
(defn get-default-url [] @atomic-default-url)

(def atomic-showing-buffers (atom false))
(defn set-showing-buffers [b] (swap! atomic-showing-buffers (fn [_] b)))
(defn get-showing-buffers? [] @atomic-showing-buffers)

(defn get-omnibar []
  (-> (get-scene-id) get-scene (.lookup "#txtURL")))

(defn get-webview []
  (-> (get-scene-id) get-scene (.lookup "#webView")))

(defn get-webengine []
  (-> (get-webview) .getEngine))

(defn -start [this stage]
  (let [
        root (FXMLLoader/load (-> "resources/WebUI.fxml" File. .toURI .toURL))
        scene (Scene. root)
        exit (reify javafx.event.EventHandler
               (handle [this event]
                 (println "Goodbye")
                 (javafx.application.Platform/exit)
                 (System/exit 0)
                 ))
        ]

    (bind-keys stage)
    (set-atomic-stage stage)
    ;; (set-scene-id 0)
    ;; (add-scene scene)
    ;; (bind-keys scene)
    (doto stage
      (.setOnCloseRequest exit)
      (.setScene scene)
      (.show))))

(defmacro run-later [& forms]
  `(let [
         p# (promise)
         ]
     (Platform/runLater
      (fn []
        (deliver p# (try ~@forms (catch Throwable t# t#)))))
     p#))

(defn execute-script [w-engine s]
  (run-later
   (let [
         result (.executeScript w-engine s)
         ]
     (if (instance? JSObject result)
       (str result)
       result))))

(defn inject-firebug [w-engine]
  (execute-script w-engine (slurp "js-src/inject-firebug.js")))

(defn execute-script-async [w-engine s]
  (let [
        p (promise)
        *out* *out*
        ]
    (Platform/runLater
     (fn []
       (let [
             o (.executeScript w-engine "new Object()")
             ]
         (.setMember o "cb" (fn [s] (deliver p s)))
         (.setMember o "println" (fn [s] (println s)))
         (.eval o s))))
    @p))

(defn repl [webengine]
  (let [s (read-line)]
    (when (not= "" (.trim s))
      (println @(execute-script webengine s))
      (recur webengine))))

(defn bind [s obj webengine]
  (run-later
   (.setMember
    (.executeScript webengine "window")
    s obj)))

(defn clear-cookies [cookie-manager]
  (-> cookie-manager .getCookieStore .removeAll))

(defmacro compile-time-slurp [file]
  (slurp file))

(def js-disable-inputs (slurp "js-src/disable-inputs.js"))

(defn async-load [url]
  (let [
        webengine (get-webengine)
        p (promise)
        f (fn [s]
            (binding [*out* *out*] (println s)))
        listener (reify ChangeListener
                   (changed [this observable old-value new-value]
                     (when (= new-value Worker$State/SUCCEEDED)
                                        ;; ;first remove this listener
                       ;; (.removeListener observable this)
                       (println "In the ChangeListener...")
                       (execute-script webengine (slurp "js-src/omnibar.js"))
                                        ;and then redefine log and error (fresh page)
                       (bind "println" f webengine)
                       (future
                         (Thread/sleep 1000)
                         ;; (execute-script webengine js-disable-inputs)
                         (execute-script webengine "console.log = function(s) {println.invoke(s)};
                                                 console.error = function(s) {println.invoke(s)};
                                                 "))
                       (deliver p true))))
        ]
    (run-later
     (doto webengine
       (-> .getLoadWorker .stateProperty (.addListener listener))
       (.load url)))
    @p))

(defn back [webengine]
  (execute-script webengine "window.history.back()"))

;; Atomic (thread safe), pretty neat.
(def key-map-current (atom :default))
(defn key-map-set [which] (swap! key-map-current (fn [_] which)))
(defn key-map-get [] @key-map-current)
;; TODO: Add numeric prefixes for repeatables

(defn keys-g-map [key]
  (case key
    "g" (do (key-map-set :default) "window.scrollTo(0, 0)")
    "o" (key-map-set :quickmarks)
    true))

(defn omnibar-stop []
  (key-map-set :default)
  (hide-buffers)
  (run-later
   (doto (get-omnibar) (.setDisable true))
   (doto (get-webview) (.setDisable false))))

(defn omnibar-start []
  (key-map-set :omnibar)
  (run-later
   (doto (get-omnibar) (.setDisable false) (.requestFocus))
   (doto (get-webview) (.setDisable true))))

;; This is basically 'escape' mode -
(defn keys-omnibar-map [key]
  (when (get-showing-buffers?)
    (filter-buffers))
  (case key
    "ENTER" (do (omnibar-stop) "hide_ob()")
    "ESCAPE" (do (omnibar-stop) "hide_ob()")
    ;; Default is to dispatch on the codes.
    (let [ccodes (map int key)]
      (println "In omnibar map with codes: ")
      (println ccodes)
      ;; Newline types
      (when (or (= '(10) ccodes)        ; ret
                (= '(13) ccodes)
                (= '(27) ccodes)        ; escape
                )
        (do (omnibar-stop) "hide_ob()"))
      true)))

(defn keys-def-map [key]
  (case key
    "g" (key-map-set :g)
    "G" "window.scrollTo(0, window.scrollY + 5000)"
    "k" "window.scrollTo(window.scrollX, window.scrollY - 50)"
    "j" "window.scrollTo(window.scrollX, window.scrollY + 50)"
    "c" "document.body.innerHTML=''"
    "r" "window.location.reload()"
    "a" "alert(1)"
    ;; "b" "confirm('you sure?')"
    ;; "o" (do (key-map-set :omnibar) (slurp "js-src/omnibar.js"))
    "O" (new-scene)
    "t" (do
          (set-new-tab true)
          (key-map-set :omnibar)
          (omnibar-start)
          "show_ob()")
    "DIGIT1" (goto-scene 0)
    "DIGIT2" (goto-scene 1)
    "DIGIT3" (goto-scene 2)
    "b" (do (key-map-set :omnibar)
            (set-showing-buffers true)
            (run-later
             (-> (get-omnibar) (.setText "")))
            (omnibar-start)
            (show-buffers))
    "o" (do (key-map-set :omnibar)
            (omnibar-start)
            "show_ob()")
    true))

;; TODO: Build this map from file
(defn keys-quickmarks-map [key]
  (key-map-set :default)
  (case key
    "a" "window.location.assign('http://ahungry.com')"
    "g" "window.location.assign('https://github.com/ahungry/ahungry-browser')"
    true))

(defn key-map-dispatcher []
  (case (key-map-get)
    :default keys-def-map
    :g keys-g-map
    :omnibar keys-omnibar-map
    :quickmarks keys-quickmarks-map
    keys-def-map))

(defn key-map-op [key]
  (let [fn-map (key-map-dispatcher)]
    (fn-map key)))

(defn key-map-handler [key]
  (let [op (key-map-op key )
        webengine (get-webengine)]
    (println (format "KM OP: %s" op))
    (when (= java.lang.String (type op))
      (execute-script webengine op))))

;; ENTER (code) vs <invis> (char), we want ENTER
;; Ideally, we want the char, since it tracks lowercase etc.
(defn get-readable-key [code text]
  (if (>= (count text) (count code))
    text code))

;; https://docs.oracle.com/javafx/2/events/filters.htm
(defn bind-keys [what]
  (doto what
    (->
     (.addEventFilter
      (. KeyEvent KEY_PRESSED)
      (reify EventHandler ;; EventHandler
        (handle [this event]
          (let [ecode (-> event .getCode .toString)
                etext (-> event .getText .toString)]
            (println (get-readable-key ecode etext))
            ;; (.consume event)
            ;; disable webview here, until some delay was met
            ;; https://stackoverflow.com/questions/27038443/javafx-disable-highlight-and-copy-mode-in-webengine
            ;; https://docs.oracle.com/javase/8/javafx/api/javafx/scene/web/WebView.html
            ;; (execute-script webengine js-disable-inputs)
            (key-map-handler (get-readable-key ecode etext)))
          ))))))

(defn url-ignore-regexes-from-file [file]
  (map re-pattern (str/split (slurp file) #"\n")))

(defn url-ignore-regexes []
  (url-ignore-regexes-from-file "conf/url-ignore-regexes.txt"))

(defn matching-regexes [url regexes]
  (filter #(re-matches % url) regexes))

(defn url-ignorable? [url]
  (let [ignorables (matching-regexes url (url-ignore-regexes))]
    (if (> (count ignorables) 0)
      (do
        (println (format "Ignoring URL: %s, hit %d matchers." url (count ignorables)))
        true)
      false)))

(defn url-or-no [url proto]
  (let [url (.toString url)]
    (URL.
     (if (url-ignorable? url)
       (format "%s://0.0.0.0:65535" proto)
       url))))

;; Hmm, we could hide things we do not want to see.
(defn my-connection-handler [protocol]
  (case protocol
    "http" (proxy [sun.net.www.protocol.http.Handler] []
             (openConnection [& [url proxy :as args]]
               (println url)
               (proxy-super openConnection (url-or-no url protocol) proxy)))
    "https" (proxy [sun.net.www.protocol.https.Handler] []
              (openConnection [& [url proxy :as args]]
                (println url)
                (proxy-super openConnection (url-or-no url protocol) proxy)))
    nil
    ))

(defn show-alert [s]
  (doto (javafx.scene.control.Dialog.)
    (-> .getDialogPane (.setContentText s))
    (-> .getDialogPane .getButtonTypes (.add (. javafx.scene.control.ButtonType OK)))
    (.showAndWait)))

(defn goto-scene [n]
  (run-later
   (set-scene-id n)
   (doto (get-atomic-stage)
     (.setScene (get-scene n))
     (.show))))

(defn omnibar-load-url [url]
  (run-later
   (if (get-new-tab?)
     (do
       (set-default-url url)
       (new-scene)
       (set-new-tab false))
     (-> (get-webengine) (.load url)))))

(defn omnibar-handler [n]
  (println "In Omnibar Handler")
  (println n)
  (let [query (cond
                (re-matches #"^http:.*" n) n
                (re-matches #".*\..*" n) (format "http://%s" n)
                :else (format "https://duckduckgo.com/lite/?q=%s" n)
                )]
    (println query)
    (omnibar-load-url query)))

(defn hide-buffers []
  (set-showing-buffers false)
  (let [bufs (-> (get-scene-id) get-scene (.lookup "#buffers"))]
    (run-later
     (-> bufs .getChildren .clear))))

(defn is-matching-buf? [s]
  (let [ob-text (-> (get-omnibar) .getText)
        pattern (re-pattern (str/lower-case (str/join "" [".*" ob-text ".*"])))]
    (re-matches pattern (str/lower-case s))))

(defn get-buffer-entry-text [scene]
  (let [webview (.lookup scene "#webView")
        engine (-> webview .getEngine)
        title (-> engine .getTitle)
        location (-> engine .getLocation)]
    (format "%s :: %s" title location)))

(defn filter-buffers []
  (let [bufs (-> (get-scene-id) get-scene (.lookup "#buffers"))
        children (-> bufs .getChildren)]

    ;; TODO: Why does this only run if we print it??
    (println (map (fn [c]
                    (when (not (is-matching-buf? (.getText c)))
                      (run-later
                       (.remove children c)
                       ))
                    (println c)
                    true) children))
    (println "Done with loop?")))

(defn show-buffers []
  (let [scenes (get-scenes)]

    (run-later
     (let [bufs (-> (get-scene-id) get-scene (.lookup "#buffers"))]
       (doto bufs
         (-> .getChildren .clear)
         (-> .getChildren (.add (Label. "Buffers: "))))))

    (map (fn [scene]
           (println "Make the scene....")
           (run-later
            (doto (-> (get-scene-id) get-scene (.lookup "#buffers"))
              (-> .getChildren (.add (Label. (get-buffer-entry-text scene)))))))
         scenes)))

(defn new-scene []
  (run-later
   (let [
         root (FXMLLoader/load (-> "resources/WebUI.fxml" File. .toURI .toURL))
         scene (Scene. root)
         ]

     (add-scene scene)
     (set-scene-id (- (count (get-scenes)) 1))
     ;; (bind-keys scene)
     ;; (set-scene-id (+ 1 (get-scene-id)))

     (println "Getting new scene, binding keys...")

     ;; Bind the keys
     (let [webview (.lookup scene "#webView")
           webengine (.getEngine webview)]

       ;; Clean up this mess
       (doto webengine

         (.setOnAlert
          (reify javafx.event.EventHandler
            (handle [this event]
              (println (.getData event))
              (show-alert (.getData event)))))

         (-> .getLoadWorker
             .stateProperty
             (.addListener
              (reify ChangeListener
                (changed [this observable old-value new-value]
                  (when (= new-value Worker$State/SUCCEEDED)
                    ;; (.removeListener observable this)
                    (println "In boot change listener")
                    (execute-script webengine (slurp "js-src/omnibar.js")))))))

         (.load (get-default-url))
         ))

     ;; Add it to the stage
     (doto (get-atomic-stage)
       (.setScene scene)
       (.show)))))

;; Abstract the webview + webengine
;; (-> (-> (get (ahungry-browser.browser/get-scenes) 0) (.lookup "#webView")) .getEngine)
