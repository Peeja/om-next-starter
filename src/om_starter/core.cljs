(ns om-starter.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om-starter.util :as util]
            [om.dom :as dom]))

(enable-console-print!)

(defn logged [name f]
  (fn [env key params]
    (js/console.debug (str "> " name "\n" (with-out-str (cljs.pprint/pprint {:env env :key key :params params}))))
    (let [ret (f env key params)]
      (js/console.debug (str "< " name "\n" (with-out-str (cljs.pprint/pprint ret))))
      ret)))

(defmulti mutate om/dispatch)

(defmulti read om/dispatch)

(defmethod read :the-list
  [{:keys [state query ast] :as env} key {:keys [remote?]}]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :the-list true}))

(defui ListItem
  static om/Ident
  (ident [this props]
    [:item/by-name (:item/name props)])
  static om/IQuery
  (query [this]
    [:item/name :item/another-basic-key])
  Object
  (render [this]
    (dom/li nil
            (pr-str (:item/name (om/props this)))
            ", "
            (pr-str (:item/another-basic-key (om/props this)))
            ", "
            (pr-str (:item/details (om/props this))))))

(def list-item (om/factory ListItem))

(defui ListOfItems
  Object
  (render [this]
    (apply dom/ul nil
           (map list-item (om/props this)))))

(def list-of-items (om/factory ListOfItems))

#_(defui Details
  Object
  (render [this]
    ))

(defui Root
  static om/IQuery
  (query [this]
    [{:the-list (om/get-query ListItem)}])
  Object
  (render [this]
    (let [{:keys [the-list]} (om/props this)]
      (dom/div nil
               (list-of-items the-list)))))

(def parser (om/parser {:read (logged "read" read) :mutate (logged "mutate" mutate)}))

(def reconciler
  (om/reconciler
   {:state (atom {})
    :merge-tree (fn [a b] (println "|merge" a b) (merge a b))
    :parser parser
    :remotes [:the-list]
    :send util/send}))

(om/add-root! reconciler Root (gdom/getElement "app"))
