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
(declare bind-keys)
(declare new-scene)
(declare goto-scene)
(declare hide-buffers)
(declare show-buffers)
(declare filter-buffers)
(declare omnibar-load-url)
(declare default-mode)
(declare omnibar-handler)
(declare omnibar-parse-command)
(declare omnibar-handle-command)

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
    :cookies {}
    :cross-domain-url ""
    :default-url (format "file://%s/docs/index.html" (System/getProperty "user.dir"))
    :hinting? false
    :mode :default
    :new-tab? false
    :omnibar-open? false
    :scene-id 0
    :scenes []
    :searching? false
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
  (let [style (case s
                "NORMAL" "-fx-text-fill: #af0; -fx-background-color: #000;"
                "OMNI" "-fx-text-fill: #000; -fx-background-color: #36f"
                "GO" "-fx-text-fill: #000; -fx-background-color: #f69"
                "INSERT" "-fx-text-fill: #000; -fx-background-color: #f36"
                "HINTING" "-fx-text-fill: #000; -fx-background-color: #f63"
                "SEARCHING" "-fx-text-fill: #000; -fx-background-color: #f33"
                "BUFFERS" "-fx-text-fill: #000; -fx-background-color: #63f"
                "-fx-text-fill: #000; -fx-background-color: #af0")]
    (run-later
     (doto (get-tip)
       (.setText s)
       (.setStyle style)))))

(defn get-omnibar-text []
  (-> (get-omnibar) .getText))

(defn set-omnibar-text [s]
  (run-later
   (doto (get-omnibar)
     (.setText s)
     (.positionCaret (count s)))))

(defn set-omnibar-text-to-url []
  (when (not (:omnibar-open? @world))
    (set-omnibar-text
     (-> (get-webengine) .getLocation))))

(defn url-ignore-regexes-from-file [file]
  (map re-pattern
       (map #(format ".*%s.*" %)
            (str/split (slurp file) #"\n"))))

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

(defn get-base-domain-pattern [s]
  (let [[_ fqdn] (re-matches #".*?://(.*?)[/.$]*" s)]
    (if fqdn
      (let [domain-parts (-> (str/split fqdn #"\.") reverse)
            domain (-> (into [] domain-parts) (subvec 0 2))]
        (if domain
          (re-pattern
           (format "^http[s]*://(.*\\.)*%s\\.%s/.*"
                   (second domain)
                   (first domain)))
          #".*")) #".*")))

;; Work with a sort of timeout here - cross domain base is set strictly after
;; first URL request, then lax again after some time has expired.
;; FIXME: Handle root domain logic better - when to flip/flop cross domain setting
;; TODO: Add cross domain user setting
(defn block-cross-domain-net?x [url]
  (let [domain (get-base-domain-pattern (:cross-domain-url @world))]
    (swap! world conj {:cross-domain-url url})
    (future (Thread/sleep 5000) (swap! world conj {:cross-domain-url ""}))
    (if (not (re-matches (re-pattern domain) url))
      (do (println (format "Blocking X-Domain request: %s" url))
          (println domain)
          true)
      false)))

(defn block-cross-domain-net? [_ ] false)

(defn url-or-no [url proto]
  (let [url (.toString url)]
    (URL.
     (if (or (url-ignorable? url) (block-cross-domain-net? url))
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

;; Opposite of slurp
(defn barf [file-name data]
  (with-open [wr (clojure.java.io/writer file-name)]
    (.write wr (pr-str data))))

(defn clean-uri [uri]
  (java.net.URI. (.getScheme uri) (.getHost uri) nil nil))

(defn cookie-to-map [cookie]
  {:name (.getName cookie)
   :value (.getValue cookie)
   :domain (.getDomain cookie)
   :maxAge (.getMaxAge cookie)
   :secure (.getSecure cookie)})

(defn cookiemap-to-cookie [{name :name value :value domain :domain maxAge :maxAge secure :secure}]
  (let [cookie (java.net.HttpCookie. name value)]
    (doto cookie
      (.setVersion 0)
      (.setDomain domain)
      (.setSecure secure)
      (.setMaxAge maxAge))))

;; Add a previously dumped cookie
(defn add-cookie [store uri cookiemap]
  (let [cookie (cookiemap-to-cookie cookiemap)
        uri (clean-uri (java.net.URI. uri))]
    (-> store (.add uri cookie))))

(defn load-cookies [store]
  (when (.exists (clojure.java.io/file "ahubu.cookies"))
    (let [cookies (read-string (slurp "ahubu.cookies"))]
      (doseq [[uri uri-map] cookies]
        (doseq [[name cookie] uri-map]
          (add-cookie store uri cookie))))))

(defn push-cookie-to-uri-map [cookie mp]
  (let [name (:name cookie)]
    (assoc mp name cookie)))

(defn push-cookie-to-cookie-map [cookie uri mp]
  (let [old (get mp uri)]
    (assoc mp uri (push-cookie-to-uri-map cookie old))))

(defn push-cookie-to-world [uri cookie]
  (swap! world
         (fn [old]
           (assoc old :cookies
                  (push-cookie-to-cookie-map cookie uri (:cookies old))))))

;; https://www.baeldung.com/cookies-java
;; https://gist.github.com/manishk3008/2a2373c6c155a5df6326
;; https://stackoverflow.com/questions/14385233/setting-a-cookie-using-javafxs-webengine-webview
(defn my-cookie-store []
  (let [store (-> (java.net.CookieManager.) .getCookieStore)
        my-store
        (proxy [java.net.CookieStore Runnable] []
          (run []
            (println "Save to disk here"))
          (add [uri cookie]
            (let [clean (clean-uri uri)
                  u (.toString clean)]
              (.add store clean cookie)
              (push-cookie-to-world u (cookie-to-map cookie))))
          (get [& [uri :as args]]
            (let [clean (clean-uri uri)
                  u (.toString clean)]
              (let [result (.get store clean)]
                result)))
          (getCookies []
            (.getCookies store))
          (getURIs []
            (.getURIs store))
          (remove [uri cookie]
            (.remove store uri cookie))
          (removeAll []
            (.removeAll store)))]
    (load-cookies my-store)
    my-store))

(defn feed-cookies-to-the-manager [manager cookies]
  (doseq [[domain domain-map] cookies]
    (doseq [[name c] domain-map]
      (let [uri (clean-uri (java.net.URI. domain))]
        (.put manager uri {"Set-Cookie" [(format "%s=%s" (:name c) (:value c)) ]})))))

(defn quietly-set-cookies []
  (def cookie-manager
    (doto (java.net.CookieManager.
           (my-cookie-store)
           java.net.CookiePolicy/ACCEPT_ALL
           ;; java.net.CookiePolicy/ACCEPT_ORIGINAL_SERVER
           )
      java.net.CookieHandler/setDefault))
  (feed-cookies-to-the-manager cookie-manager (:cookies @world)))

(defn save-cookies []
  (barf "ahubu.cookies" (:cookies @world)))

(defn dump-cookies [store]
  (doall (map cookie-to-map (.getCookies store))))

(defn quietly-set-stream-factory []
  (WebUIController/stfuAndSetURLStreamHandlerFactory))

(defn -start [this stage]
  (let [
        root (FXMLLoader/load (-> "resources/WebUI.fxml" File. .toURI .toURL))
        scene (Scene. root)
        exit (reify javafx.event.EventHandler
               (handle [this event]
                 (println "Goodbye")
                 (save-cookies)
                 (javafx.application.Platform/exit)
                 (System/exit 0)
                 ))
        ]

    (bind-keys stage)
    (set-atomic-stage stage)

    ;; (.addShutdownHook
    ;;  (java.lang.Runtime/getRuntime)
    ;;  (Thread. (println "Adios!") (save-cookies)))

    ;; (set-scene-id 0)
    ;; (add-scene scene)
    ;; (bind-keys scene)
    (doto stage
      (.setOnCloseRequest exit)
      (.setScene scene)
      (.setTitle "AHUBU")
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
  (run-later
   (doto (get-webengine)
     (.load url))))

(defn async-loadx [url]
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

(defn prev-scene []
  (default-mode)
  (let [n (get-scene-id)
        id (- n 1)]
    (if (< id 0)
      (goto-scene (- (count (get-scenes)) 1))
      (goto-scene id))))

(defn next-scene []
  (default-mode)
  (let [n (get-scene-id)
        id (+ n 1)]
    (if (>= id (count (get-scenes)))
      (goto-scene 0)
      (goto-scene id))))

(defn omnibar-stop []
  (swap! world conj {:omnibar-open? false})
  (run-later
   (future (Thread/sleep 100) (set-omnibar-text-to-url))
   (doto (get-omnibar) (.setDisable true))
   (doto (get-webview) (.setDisable false))))

(defn omnibar-start []
  (swap! world conj {:omnibar-open? true})
  (run-later
   (doto (get-omnibar) (.setDisable false) (.requestFocus))
   (doto (get-webview) (.setDisable true))))

(defn yank [s]
  (let [content (ClipboardContent.)]
    (run-later
     (set-tip "YANKED!")
     (future (Thread/sleep 500) (set-tip "NORMAL"))
     (-> content (.putString s))
     (-> (Clipboard/getSystemClipboard) (.setContent content)))))

(defn yank-current-url []
  (-> (get-webengine) .getLocation yank))

(defn buffers-start []
  (set-mode :omnibar)
  (set-tip "BUFFERS")
  (set-showing-buffers true)
  (run-later
   (omnibar-start)
   (show-buffers)
   (set-omnibar-text ":buffers! ")
   "Overlay.show()"))

(defn quickmark-url [url]
  (default-mode)
  (omnibar-load-url url))

(defn get-xdg-config-home []
  (or (System/getenv "XDG_CONFIG_HOME")
      (System/getProperty "user.home")))

(defn get-rc-file-raw []
  (let [defaults (read-string (slurp "conf/default-rc"))
        home-rc (format "%s/.ahuburc" (System/getProperty "user.home"))
        xdg-rc (format "%s/ahubu/ahuburc" (get-xdg-config-home))]
    (conj
      defaults
      (if (.exists (clojure.java.io/file home-rc))
        (read-string (slurp home-rc)))
      (if (.exists (clojure.java.io/file xdg-rc))
        (read-string (slurp xdg-rc))))))

(defn get-rc-file []
  (let [rc (get-rc-file-raw)
        quickmarks (:quickmarks rc)
        qm-fns (reduce-kv #(assoc %1 %2 (fn [] (quickmark-url %3))) {} quickmarks)
        merged-qms (conj (:quickmarks (:keymaps rc)) qm-fns)]
    (conj rc
          {:keymaps (conj (:keymaps rc)
                          {:quickmarks merged-qms})})))

(defn go-mode []
  (set-mode :go)
  (set-tip "GO"))

(defn font-mode []
  (set-mode :font)
  (set-tip "FONT"))

(defn quickmarks-mode []
  (set-mode :quickmarks)
  (set-tip "QUICKMARKS"))

(defn quickmarks-new-tab-mode []
  (set-new-tab true)
  (quickmarks-mode))

(defn default-mode []
  (set-mode :default)
  (set-tip "NORMAL")
  (hide-buffers)
  (omnibar-stop)
  (swap! world conj {:hinting? false :searching? false})
  (dojs "Hinting.off(); Overlay.hide(); Form.disable()"))

(defn insert-mode []
  (set-mode :insert)
  (set-tip "INSERT")
  (dojs "Form.enable()"))

(defn search-mode []
  (set-mode :search)
  (set-tip "SEARCHING")
  (swap! world conj {:searching? true})
  (println "Searching")
  (set-omnibar-text "/")
  (dojs "Search.reset()"))

(defn hinting-mode []
  (set-mode :hinting)
  (set-tip "HINTING")
  (swap! world conj {:hinting? true})
  (dojs "Hinting.on(); Overlay.show()"))

(defn inject-firebug []
  (dojsf "inject-firebug"))

(defn omnibar-open []
  (set-mode :omnibar)
  (set-tip "OMNI")
  (omnibar-start)
  (set-omnibar-text ":open ")
  (dojs "Overlay.show()"))

(defn omnibar-open-current []
  (omnibar-open)
  (set-omnibar-text (format  ":open %s" (get-omnibar-text))))

(defn omnibar-open-new-tab []
  (set-new-tab true)
  (omnibar-open)
  (set-omnibar-text ":tabopen "))

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
    op))

(defn process-op [op]
  (when op
    (if (= java.lang.String (type op))
      (execute-script (get-webengine) op)
      ((eval op)))))

(defn key-map-handler [key]
  (let [op (key-map-op key)
        op-before (key-map-op :BEFORE)
        op-after (key-map-op :AFTER)]

    ;; (println (format "KM OP: %s" op-before))
    ;; (println key)
    ;; (println (format "KM OP: %s" op))
    ;; (println (format "KM OP: %s" op-after))

    ;; Global key listeners
    (when (get-showing-buffers?)
      (filter-buffers))

    (when (:hinting? @world)
      (dojs (format "Hinting.keyHandler('%s')" key))
      ;; (println (format  "HINTING: %s" key))
      )

    (when (:searching? @world)
      (when (= 1 (count key))
        (set-omnibar-text (format "%s%s" (get-omnibar-text) key)))
      (dojs (format "Search.incrementalFind('%s')" key))
      )

    ;; Check for the BEFORE bind (runs with any other keypress)
    (process-op op-before)
    (process-op op)
    (future
      (Thread/sleep 100)
      (process-op op-after))

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
            ;; (println (get-readable-key ecode etext))
            ;; (.consume event)
            ;; disable webview here, until some delay was met
            ;; https://stackoverflow.com/questions/27038443/javafx-disable-highlight-and-copy-mode-in-webengine
            ;; https://docs.oracle.com/javase/8/javafx/api/javafx/scene/web/WebView.html
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

(defn omnibar-parse-command [cmd]
  (re-matches #":(.*?) (.*)" cmd))

(defn omnibar-handle-command [cmd]
  (let [[_ cmd arg] (omnibar-parse-command cmd)]
    ;; (println (format "OB Parse Cmd: %s %s %s" _ cmd arg))
    (case cmd
      "open" (omnibar-handler arg)
      "tabopen" (omnibar-handler arg)
      (omnibar-handler _))))

(defn omnibar-handler [n]
  (if (get-showing-buffers?) (switch-to-buffer)
      (let [query
            (cond
              (re-matches #"^:.*" n) (omnibar-handle-command n)
              (re-matches #"^file:.*" n) n
              (re-matches #"^http[s]*:.*" n) n
              (re-matches #".*\..*" n) (format "http://%s" n)
              :else (format "https://duckduckgo.com/lite/?q=%s" n)
              )]
        (omnibar-load-url query))))

(defn hide-buffers []
  (let [bufs (get-buffers)]
    (run-later
     (-> bufs .getChildren .clear))))

(defn is-matching-buf? [s]
  (let [[_ cmd arg] (-> (get-omnibar) .getText omnibar-parse-command)
        ob-text (or arg _)
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
    ;; (Thread/sleep 100)
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
            ;; (println (-> el .getTextContent))
            ;; (println (-> el (.getAttribute "href")))

            (-> el (.addEventListener
                    "click"
                    (reify org.w3c.dom.events.EventListener
                      (handleEvent [this event]
                        (default-mode)
                        (println "I clicked a link, good job")
                        (println (-> el .getTextContent))))
                    false))

            )

            )
        (range (.getLength els)))))

(defn remove-annoying-div [dom id]
  (let [el (-> dom (.getElementById id))]
    (when el (.remove el))))

(defn remove-annoying-divs [dom]
  (let [ids (str/split (slurp "conf/dom-id-ignores.txt") #"\n")]
    (doseq [id ids]
      (remove-annoying-div id))))

(defn remove-annoying-class [dom class-name]
  (let [els (-> dom (.getElementsByClassName class-name))]
    (doseq [_ (range (.getLength els))]
      ;; We remove item 0, because each remove causes a reindex
      (let [el (-> els (.item 0))]
        (-> el .remove)))))

(defn remove-annoying-classes [dom]
  (let [ids (str/split (slurp "conf/dom-class-ignores.txt") #"\n")]
    (doseq [id ids]
      (remove-annoying-class dom id))))

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

         ;; (.onStatusChanged
         ;;  (reify javafx.event.EventHandler
         ;;    (handle [this event]
         ;;      (println "On status change"))))

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

                  (when (and (= new-value Worker$State/RUNNING)
                             (= old-value Worker$State/SCHEDULED))
                    (execute-script webengine js-bundle))

                  (when (not (= new-value Worker$State/SUCCEEDED))
                    (set-omnibar-text
                     (format "Loading :: %s" (-> webengine .getLocation))))

                  (when (= new-value Worker$State/SUCCEEDED)
                    ;; (.removeListener observable this)
                    ;; https://docs.oracle.com/javase/8/javafx/api/javafx/scene/web/WebEngine.html
                    (println (-> webengine .getLocation))
                    ;; (println (-> webengine .getDocument .toString))

                    ;; When a thing loads, set the URL to match
                    (set-omnibar-text-to-url)

                    ;; map over all the page links on load
                    (-> webengine .getDocument remove-annoying-divs)
                    (-> webengine .getDocument remove-annoying-classes)
                    (-> webengine .getDocument (.getElementsByTagName "a") el-link-fn)
                    (-> webengine (.setUserAgent "Mozilla/5.0 (Windows NT 6.1) Gecko/20100101 Firefox/61.0"))

                    ;; (-> webengine .getDocument (.getElementById "content")
                    ;;     (.addEventListener
                    ;;      "click"
                    ;;      (reify org.w3c.dom.events.EventListener
                    ;;        (handleEvent [this event]
                    ;;          (javafx.application.Platform/exit)))))

                    (execute-script webengine js-bundle)
                    )))))

         (.load (get-default-url))
         ))

     ;; Add it to the stage
     (doto (get-atomic-stage)
       (.setScene scene)
       (.show)))))

;; Abstract the webview + webengine
;; (-> (-> (get (ahubu.browser/get-scenes) 0) (.lookup "#webView")) .getEngine)
