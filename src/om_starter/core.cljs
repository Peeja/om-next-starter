(ns om-starter.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om-starter.util :as util]
            [om.dom :as dom]
            [sablono.core :refer-macros [html]]
            cljs.pprint))

(enable-console-print!)


(defprotocol IQueryDelegate
  (query-delegate [c]))

(defn get-query [x]
  (if (implements? IQueryDelegate x)
    (om/get-query (query-delegate x))
    (om/get-query x)))



(defmulti mutate om/dispatch)

(defmulti read om/dispatch)

(defmethod read :the-list
  [{:keys [state query ast] :as env} key {:keys [remote?]}]
  (let [st @state]
    (if-let [v (seq (om/db->tree query (get st key) st))]
      {:value v}
      {:value [] :the-list true})))

(defmethod read :item/by-name
  [{:keys [state query query-root ast] :as env} key {:keys [remote?]}]
  (let [st @state]
    {:value (om/db->tree query query-root st)
     :item ast}))

(defui ListItem
  static om/Ident
  (ident [this props]
    [:item/by-name (:item/name props)])
  static om/IQuery
  (query [this]
    [:item/name :item/another-basic-key])
  Object
  (render [this]
    (html
     [:li {:on-click (partial (:on-click (om/get-computed this)) this)}
      (pr-str (:item/name (om/props this)))
      ", "
      (pr-str (:item/another-basic-key (om/props this)))
      ", "
      (pr-str (:item/details (om/props this)))])))

(def list-item (om/factory ListItem))

(defui ListOfItems
  static IQueryDelegate
  (query-delegate [this] ListItem)
  Object
  (render [this]
    (html
     [:ul
      (for [item (om/props this)]
        (list-item (om/computed item
                                {:on-click (:on-item-click (om/get-computed this))})))])))

(def list-of-items (om/factory ListOfItems))

(defui Details
  static om/Ident
  (ident [this props]
    [:item/by-name (:item/name props)])
  static om/IQuery
  (query [this]
    [:item/name :item/details])
  Object
  (render [this]
    (dom/div nil
             (dom/p nil
                    "Selected: "
                    (:item/name (om/props this)))
             (dom/p nil
                    "Details: "
                    (:item/details (om/props this))))))

(def details (om/factory Details))

(defui Root
  static om/IQueryParams
  (params [this]
    {:selected-item-ident [:item/by-name "B"]})
  static om/IQuery
  (query [this]
    [{:the-list (get-query ListOfItems)}
     {'?selected-item-ident (get-query Details)}])
  Object
  (render [this]
    (let [{:keys [the-list]} (om/props this)
          {:keys [selected-item-ident]} (om/get-params this)
          selected-item (get (om/props this) selected-item-ident)]
      (dom/div nil
               (list-of-items (om/computed the-list
                                           {:on-item-click #(om/set-query! this {:params {:selected-item-ident (om/get-ident %)}})}))
               (details selected-item)))))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
   {:state {}
    :merge-tree (fn [a b] (println "|merge" a b) (merge a b))
    :merge-ident (fn [something tree ref props]
                   (js/console.info (with-out-str (cljs.pprint/pprint [tree ref props])))
                   (let [result (update-in tree ref merge props)]
                     (js/console.info (with-out-str (cljs.pprint/pprint result)))
                     result))
    :parser parser
    :remotes [:the-list :item]
    :send util/send}))

(om/add-root! reconciler Root (gdom/getElement "app"))
