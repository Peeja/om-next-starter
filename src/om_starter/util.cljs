(ns om-starter.util
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put!]]
            [cognitect.transit :as t])
  (:import [goog.net XhrIo]))

(defn hidden [is-hidden]
  (if is-hidden
    #js {:display "none"}
    #js {}))

(defn pluralize [n word]
  (if (== n 1)
    word
    (str word "s")))

(defn transit-post [url]
  (fn [edn cb]
    (.send XhrIo url
           (fn [e]
             (this-as this
               (cb (t/read (t/reader :json) (.getResponseText this)))))
           "POST" (t/write (t/writer :json) edn)
           #js {"Content-Type" "application/transit+json"})))

(defn transit-post-chan [url edn]
  (let [c (chan)]
    ((transit-post url) edn (fn [res] (put! c res)))
    c))

(defn transit-get [url cb]
  (.send XhrIo url
         (fn [e]
           (when (.. e -target isSuccess)
             (this-as this
               (cb (t/read (t/reader :json) (.getResponseText this))))))))

(defmulti send* key)

(defmethod send* :the-list
  [[_ query] cb]
  (transit-get "/the-list" #(cb {:the-list %})))

(defn send [remotes cb]
  (doseq [remote-entry remotes]
    (send* remote-entry cb)))

(comment
  (def sel [{:todos/list [:db/id :todo/title :todo/completed :todo/created]}])

  (t/write (t/writer :json) sel)

  (go (println (<! (transit-post-chan "/api" sel))))
  )
