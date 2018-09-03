(ns ahubu.lib
  (:require
   [clojure.string :as str])
  (:import
   MyEventDispatcher
   WebUIController
   (java.io File)
   (java.net HttpURLConnection URL URLConnection URLStreamHandler URLStreamHandlerFactory)
   (javafx.application Application Platform)
   (javafx.beans.value ChangeListener)
   (javafx.concurrent Worker$State)
   (javafx.event EventHandler)
   (javafx.fxml FXMLLoader)
   (javafx.scene Parent Scene)
   (javafx.scene.control Label)
   (javafx.scene.input Clipboard ClipboardContent KeyEvent)
   (javafx.scene.web WebView)
   (javafx.stage Stage)
   (javax.net.ssl HttpsURLConnection)
   (netscape.javascript JSObject)
   (sun.net.www.protocol.https Handler)
   ))

(gen-class
 :extends javafx.application.Application
 :name com.ahungry.Browser)

(declare delete-current-scene)
(declare keys-g-map)
(declare keys-default)
(declare bind-keys)
(declare new-scene)
(declare goto-scene)
(declare hide-buffers)
(declare show-buffers)
(declare filter-buffers)
(declare omnibar-load-url)
(declare default-mode)

(defmacro compile-time-slurp [file]
  (slurp file))

(def js-bundle (slurp "js-src/bundle.js"))

(defmacro run-later [& forms]
  `(let [
         p# (promise)
         ]
     (Platform/runLater
      (fn []
        (deliver p# (try ~@forms (catch Throwable t# t#)))))
     p#))

(def world
  (atom
   {
    :default-url (format "file://%s/docs/index.html" (System/getProperty "user.dir"))
    :mode :default
    :new-tab? false
    :omnibar-open? false
    :scene-id 0
    :scenes []
    :showing-buffers? false
    :stage nil
    }))

(defn set-mode [mode]
  (swap! world conj {:mode mode}))

(defn set-atomic-stage [stage]
  (swap! world conj {:stage stage}))
(defn get-atomic-stage [] (:stage @world))

;; Each scene is basically a tab
(defn add-scene [scene]
  (swap! world conj {:scenes (conj (:scenes @world) scene)}))
(defn get-scene [n]
  (-> (:scenes @world) (get n)))
(defn get-scenes [] (:scenes @world))
(defn delete-nth-scene [scenes n]
  (into []
        (concat (subvec scenes 0 n)
                (subvec scenes (+ 1 n) (count scenes)))))
(defn del-scene [n]
  (swap! world conj {:scenes (-> (:scenes @world) (delete-nth-scene n))}))

(defn set-scene-id [n] (swap! world conj {:scene-id n}))
(defn get-scene-id [] (:scene-id @world))

(defn set-new-tab [b]
  (swap! world conj {:new-tab? b}))
(defn get-new-tab? [] (:new-tab? @world))

;; (def atomic-default-url (atom "http://ahungry.com"))
(defn set-default-url [s]
  (swap! world conj {:default-url s}))
(defn get-default-url [] (:default-url @world))

(defn set-showing-buffers [b]
  (swap! world conj {:showing-buffers? b}))
(defn get-showing-buffers? [] (:showing-buffers? @world))

(defn get-omnibar []
  (-> (get-scene-id) get-scene (.lookup "#txtURL")))

(defn get-webview []
  (-> (get-scene-id) get-scene (.lookup "#webView")))

(defn get-webengine []
  (-> (get-webview) .getEngine))

(defn get-buffers []
  (-> (get-scene-id) get-scene (.lookup "#buffers")))

(defn get-tip []
  (-> (get-scene-id) get-scene (.lookup "#tip")))

(defn set-tip [s]
  (run-later
   (-> (get-tip) (.setText s))))

(defn set-omnibar-text [s]
  (run-later
   (-> (get-omnibar) (.setText s))))

(defn set-omnibar-text-to-url []
  (when (not (:omnibar-open? @world))
    (set-omnibar-text
     (-> (get-webengine) .getLocation))))

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

(defn quietly-set-cookies []
  (def cookie-manager
    (doto (java.net.CookieManager.)
      java.net.CookieHandler/setDefault)))

(defn quietly-set-stream-factory []
  (try
    (def stream-handler-factory
      (URL/setURLStreamHandlerFactory
       (reify URLStreamHandlerFactory
         (createURLStreamHandler [this protocol] (#'my-connection-handler protocol)))))
    (catch Throwable e
      ;; TODO: Attempt to force set with reflection maybe - although this is usually good enough.
      ;; TODO: Make sure this isn't some big performance penalty.
      )))

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

(defn execute-script [w-engine s]
  (run-later
   (let [
         result (.executeScript w-engine s)
         ]
     (if (instance? JSObject result)
       (str result)
       result))))

(defn dojs [s ]
  (execute-script (get-webengine) s))

(defn dojsf [file]
  (execute-script (get-webengine) (slurp (format "js-src/%s.js" file))))

(defn decrease-font-size []
  (dojsf "decrease-font-size"))

(defn increase-font-size []
  (dojsf "increase-font-size"))

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
                       (execute-script webengine js-bundle)
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

(defn prev-scene []
  (let [n (get-scene-id)
        id (- n 1)]
    (if (< id 0)
      (goto-scene (- (count (get-scenes)) 1))
      (goto-scene id))))

(defn next-scene []
  (let [n (get-scene-id)
        id (+ n 1)]
    (if (>= id (count (get-scenes)))
      (goto-scene 0)
      (goto-scene id))))

(defn keys-g-map [key]
  (case key
    "g" (do (key-map-set :default) "window.scrollTo(0, 0)")
    "i" (do (set-tip "INSERT") (key-map-set :insert) "Form.enable()")
    "o" (key-map-set :quickmarks)
    "n" (do
          (set-new-tab true)
          (key-map-set :quickmarks))
    "T" (do (key-map-set :default) (prev-scene))
    "t" (do (key-map-set :default) (next-scene))
    true))

(defn omnibar-stop []
  (key-map-set :default)
  (swap! world conj {:omnibar-open? false})
  (run-later
   (future (Thread/sleep 100) (set-omnibar-text-to-url))
   (doto (get-omnibar) (.setDisable true))
   (doto (get-webview) (.setDisable false))))

(defn omnibar-start []
  (key-map-set :omnibar)
  (swap! world conj {:omnibar-open? true})
  (run-later
   (doto (get-omnibar) (.setDisable false) (.requestFocus))
   (doto (get-webview) (.setDisable true))))

;; TODO: Timing event - Hinting.off should run after js link visit does
(defn keys-hinting-map [key]
  (case key
    "ESCAPE" (do (set-tip "NORMAL") (key-map-set :default) "Overlay.hide(); Hinting.off(); ")
    (do (set-tip "NORMAL") (key-map-set :default) "Overlay.hide(); setTimeout(Hinting.off, 200)")))

(defn keys-insert-map [key]
  (case key
    "ESCAPE" (do (set-tip "NORMAL") (key-map-set :default) "Form.disable()")
    true))

(defn keys-fontsize-map [key]
  (key-map-set :default)
  (case key
    "o" (decrease-font-size)
    "i" (increase-font-size)
    true))

;; This is basically 'escape' mode -
(defn keys-omnibar-map [key]
  (when (get-showing-buffers?)
    (filter-buffers))
  (case key
    "ENTER" (do (omnibar-stop) (set-tip "NORMAL") "Overlay.hide()")
    "ESCAPE" (do (set-showing-buffers false) (hide-buffers) (omnibar-stop) (set-tip "NORMAL") "Overlay.hide()")
    ;; Default is to dispatch on the codes.
    (let [ccodes (map int key)]
      (println "In omnibar map with codes: ")
      (println ccodes)
      ;; Newline types
      (when (or (= '(10) ccodes)        ; ret
                (= '(13) ccodes)
                (= '(27) ccodes)        ; escape
                )
        (do (omnibar-stop) (set-tip "NORMAL") "Overlay.hide()"))
      true)))

(defn yank [s]
  (let [content (ClipboardContent.)]
    (run-later
     (-> content (.putString s))
     (-> (Clipboard/getSystemClipboard) (.setContent content)))))

(defn yank-current-url []
  (-> (get-webengine) .getLocation yank))

(defn keys-def-map [key]
  (case key
    "g" (key-map-set :g)
    "d" (delete-current-scene)
    "y" (yank-current-url)
    "G" "window.scrollTo(0, window.scrollY + 5000)"
    "z" (key-map-set :fontsize)
    "f" (do (set-tip "HINTING") (key-map-set :hinting) "Hinting.on(); Overlay.show()" )
    "F12" (slurp "js-src/inject-firebug.js")
    "k" "window.scrollTo(window.scrollX, window.scrollY - 50)"
    "j" "window.scrollTo(window.scrollX, window.scrollY + 50)"
    "h" "window.scrollTo(window.scrollX - 50, window.scrollY)"
    "l" "window.scrollTo(window.scrollX + 50, window.scrollY)"
    "c" "document.body.innerHTML=''"
    "r" "window.location.reload()"
    "a" "alert(1)"
    "" "window.history.back()"        ; C-o
    "	" "window.history.forward()"    ; C-i
    ;; "b" "confirm('you sure?')"
    ;; "o" (do (key-map-set :omnibar) (slurp "js-src/omnibar.js"))
    "O" (new-scene)
    "t" (do
          (set-new-tab true)
          (key-map-set :omnibar)
          (omnibar-start)
          "Overlay.show()")
    "DIGIT1" (goto-scene 0)
    "DIGIT2" (goto-scene 1)
    "DIGIT3" (goto-scene 2)
    "b" (do (key-map-set :omnibar)
            (set-tip "BUFFERS")
            (set-showing-buffers true)
            (run-later
             (-> (get-omnibar) (.setText "")))
            (omnibar-start)
            (show-buffers)
            "Overlay.show()")
    "o" (do (key-map-set :omnibar)
            (set-tip "OMNI")
            (omnibar-start)
            "Overlay.show()")
    ;; TODO: Hmm, if we return false, it does not seem to bubble
    true))

(defn quickmark-url [url]
  (default-mode)
  (omnibar-load-url url))

(defn get-rc-file []
  (let [defaults (read-string (slurp "conf/default-rc"))]
    (try
      (conj
       defaults
       (read-string (slurp (format "%s/.ahuburc" (System/getProperty "user.home")))))
      (catch Throwable t
        (println t)
        defaults))))

(defn keys-quickmarks-map [key]
  (key-map-set :default)
  (let [rc (:quickmarks (get-rc-file))
        url (get rc (keyword key))]
    (quickmark-url url))
  true)

(defn go-mode []
  (set-mode :go)
  (set-tip "GO"))

(defn font-mode []
  (set-mode :font)
  (set-tip "FONT"))

(defn quickmarks-mode []
  (set-tip "QUICKMARKS")
  (set-mode :quickmarks))

(defn default-mode []
  (set-mode :default)
  (set-tip "NORMAL")
  (omnibar-stop)
  (dojs "Hinting.off(); Overlay.hide()"))

(defn hinting-mode []
  (set-mode :hinting)
  (set-tip "HINTING")
  (dojs "Hinting.on(); Overlay.show()"))

(defn inject-firebug []
  (dojsf "inject-firebug"))

(defn omnibar-open []
  (set-mode :omnibar)
  (set-tip "OMNI")
  (omnibar-start)
  (dojs "Overlay.show()"))

(defn omnibar-open-new-tab []
  (set-new-tab true)
  (omnibar-open))

(defn go-top []
  (default-mode)
  (dojs "window.scrollTo(0, 0)"))

;; Try to grab string key, then keyword key
(defn key-map-op [key]
  (let [mode (:mode @world)
        rc (-> (:keymaps (get-rc-file)) (get mode))
        op? (get rc key)
        key (keyword key)
        op (or op? (get rc key))]
    (println rc)
    op))

(defn key-map-handler [key]
  (let [op (key-map-op key )
        webengine (get-webengine)]
    (println (format "KM OP: %s" op))
    (when op
      (if (= java.lang.String (type op))
        (execute-script webengine op)
        ((eval op))))
    true))                              ; bubble up keypress

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
          false
          ))))))

(defn show-alert [s]
  (doto (javafx.scene.control.Dialog.)
    (-> .getDialogPane (.setContentText s))
    (-> .getDialogPane .getButtonTypes (.add (. javafx.scene.control.ButtonType OK)))
    (.showAndWait)))

(defn goto-scene [n]
  (println "GOING TO SCENE")
  (println n)
  (run-later
   (set-scene-id n)
   (doto (get-atomic-stage)
     (.setScene (get-scene n))
     (.show))))

(defn delete-current-scene []
  (let [n (get-scene-id)]
    (when (> n 0)
      (goto-scene (- n 1))
      (run-later
       (Thread/sleep 50)
       (del-scene n)))))

(defn omnibar-load-url [url]
  (run-later
   (if (get-new-tab?)
     (do
       (set-default-url url)
       (new-scene)
       (set-new-tab false))
     (-> (get-webengine) (.load url)))))

(defn get-selected-buffer-text []
  (let [bufs (get-buffers)
        children (-> bufs .getChildren)
        id 0
        child (when children (get (vec children) id))]
    (if child (.getText child) "")))

(defn switch-to-buffer []
  (let [s (get-selected-buffer-text)
        maybe-id (last (re-matches #"^([0-9]).*" s))
        id (if maybe-id (Integer/parseInt maybe-id) -1)]
    (when (>= id 0)
      (goto-scene id))
    (set-showing-buffers false)
    (hide-buffers)))

(defn omnibar-handler [n]
  (if (get-showing-buffers?) (switch-to-buffer)
      (let [query
            (cond
              (re-matches #"^file:.*" n) n
              (re-matches #"^http:.*" n) n
              (re-matches #".*\..*" n) (format "http://%s" n)
              :else (format "https://duckduckgo.com/lite/?q=%s" n)
              )]
        (omnibar-load-url query))))

(defn hide-buffers []
  (let [bufs (get-buffers)]
    (run-later
     (-> bufs .getChildren .clear))))

(defn is-matching-buf? [s]
  (let [ob-text (-> (get-omnibar) .getText)
        pattern (re-pattern (str/lower-case (str/join "" [".*" ob-text ".*"])))]
    (re-matches pattern (str/lower-case s))))

(defn get-buffer-entry-text [scene n]
  (let [webview (.lookup scene "#webView")
        engine (-> webview .getEngine)
        title (-> engine .getTitle)
        location (-> engine .getLocation)]
    (format "%s :: %s :: %s" n title location)))

(defn filter-buffers []
  (future
    (Thread/sleep 100)
    (let [bufs (get-buffers)
          children (-> bufs .getChildren)]

      (doall
       (map
        (fn [c]
          (when (not (is-matching-buf? (.getText c)))
            (run-later
             (.remove children c)
             )))
        children)))))

(defn show-buffers []
  (let [scenes (get-scenes)]

    (run-later
     (let [bufs (get-buffers)]
       (doto bufs
         (-> .getChildren .clear)
         (-> .getChildren (.add (Label. "Buffers: "))))))

    (doall
     (map (fn [i]
            (let [scene (get scenes i)]
              (println "Make the scene....")
              (run-later
               (doto (-> (get-scene-id) get-scene (.lookup "#buffers"))
                 (-> .getChildren (.add (Label. (get-buffer-entry-text scene i))))))))
          (range (count scenes))))))

;; Map over elements (links) on page load...sweet
;; TODO: Based on this filter list, we can show the user a native list
;; of jumpable links (instead of relying on JS), where it works like the buffer
;; jump list, but the action is set to .load url or simulate a key click
(defn el-link-fn [els]
  (doall
   (map (fn [i]
          (let [el (-> els (.item i))]
            ;; https://docs.oracle.com/cd/E13222_01/wls/docs61/xerces/org/apache/html/dom/HTMLAnchorElementImpl.html
            ;; (-> el (.setTextContent  "OH WEL"))
            (println (-> el .getTextContent))
            (println (-> el (.getAttribute "href")))

            (-> el (.addEventListener
                    "click"
                    (reify org.w3c.dom.events.EventListener
                      (handleEvent [this event]
                        (println "I clicked a link, good job")
                        (println (-> el .getTextContent))))
                    false))

            )

            )
        (range (.getLength els)))))

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
                    ;; https://docs.oracle.com/javase/8/javafx/api/javafx/scene/web/WebEngine.html
                    (println (-> webengine .getLocation))
                    (println (-> webengine .getDocument .toString))

                    ;; When a thing loads, set the URL to match
                    (set-omnibar-text-to-url)

                    ;; map over all the page links on load
                    (-> webengine .getDocument (.getElementsByTagName "a") el-link-fn)

                    (-> webengine (.setUserAgent "Mozilla/5.0 (Windows NT 6.1) Gecko/20100101 Firefox/61.0"))

                    ;; (-> webengine .getDocument (.getElementById "content")
                    ;;     (.addEventListener
                    ;;      "click"
                    ;;      (reify org.w3c.dom.events.EventListener
                    ;;        (handleEvent [this event]
                    ;;          (javafx.application.Platform/exit)))))

                    (execute-script webengine js-bundle))))))

         (.load (get-default-url))
         ))

     ;; Add it to the stage
     (doto (get-atomic-stage)
       (.setScene scene)
       (.show)))))

;; Abstract the webview + webengine
;; (-> (-> (get (ahubu.browser/get-scenes) 0) (.lookup "#webView")) .getEngine)
