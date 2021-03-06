(ns mydic.main
  (:require [cljs.nodejs :as nodejs]))


(def Electron (nodejs/require "electron"))
(def app (.-app Electron))
(def BrowserWindow (.-BrowserWindow Electron))
(def globalShortcut (.-globalShortcut Electron))
(def clipboard (.-clipboard Electron))
(def path (nodejs/require "path"))
(def url (nodejs/require "url"))
(def fs (nodejs/require "fs"))
(def config
  (-> fs
      (.readFileSync (.join path
                            (.resolve path ".")
                            "config.json"))
      js/JSON.parse
      js->clj))

(defonce *win* (atom nil))
(def visible (atom nil))

(def darwin? (= (.-platform nodejs/process) "darwin"))

(defn create-window []
  (reset! *win* (BrowserWindow. (clj->js {:width 800 :height 600})))
  (let [u (.format url (clj->js {:pathname (.join path
                                                  (.resolve path ".")
                                                  "resources"
                                                  "public"
                                                  "index.html")
                                 :protocol "file:"
                                 :slashes true}))]
    (.loadURL @*win* u))
  (.on app "closed" (fn [] (reset! *win* nil))))


(defn register-global-shortcut []
  (.register globalShortcut
             (or (get-in config ["keymap" "global-search-shortcut"])
                 "CommandOrControl+F1")
             (fn []
               (-> @*win*
                   (.-webContents)
                   (.send "find-word" (.readText clipboard)))
               (.readText clipboard)
               (.show @*win*))))


(defn unregister-global-shortcut []
  (.unregister globalShortcut "CommandOrControl+F1")
  (.unregisterAll globalShortcut))

(defn redefine-global-shortcut []
  (unregister-global-shortcut)
  (register-global-shortcut))

(defn -main []
  (doto app
    (.on "ready" (fn []
                   (println "Printing something to make electron work with figwheel")
                   (create-window)
                   (reset! visible true)
                   (register-global-shortcut)))

    (.on "window-all-closed"
         (fn [] (when-not darwin? (.quit app))))

    (.on "activate"
         (fn [] (when darwin? (create-window))))

    (.on "will-quit"
         (fn [] (unregister-global-shortcut)))))

(nodejs/enable-util-print!)
(.log js/console "App has started!")
(set! *main-cli-fn* -main)
