(ns om-starter.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om-starter.util :as util]
            [om.dom :as dom]))

(enable-console-print!)

(defmulti mutate om/dispatch)

(defmethod mutate 'app/update-title
  [{:keys [state]} _ {:keys [new-title]}]
  {:remote true
   :value [:app/title]
   :action (fn [] (swap! state assoc :app/title new-title))})

(defmethod mutate 'app/loading?
  [{:keys [state]} _ _]
  {:value [:loading?]
   :action (fn [] (swap! state assoc :loading? true))})

(defmulti read om/dispatch)

(defmethod read :app/title
  [{:keys [state] :as env} _ {:keys [remote?]}]
  (let [st @state]
    (if-let [v (get st :app/title)]
      {:value v :remote true}
      {:remote true})))

(defmethod read :loading?
  [{:keys [state] :as env} _ _]
  (let [st @state]
    (let [v (get st :loading? false)]
      (if v
        {:value v :remote true}
        {:value v}))))

(defui FancyButtonWithClass
  Object
  (render [this]
    (dom/button #js {:className "fancy"
                     :onClick (:onClick (om/props this))}
                (om/children this))))

(def fancy-button-with-class (om/factory FancyButtonWithClass))

(defn fancy-button-with-function-call [props children]
  (dom/button #js {:className "fancy"
                   :onClick (:onClick props)}
              children))

(defn fancy-button-with-function-as-component [props]
  (dom/button #js {:className "fancy"
                   :onClick (.-onClick props)}
              (.-children props)))

(def fancy-button-with-function-as-component-factory (js/React.createFactory fancy-button-with-function-as-component))

(defui Root
  static om/IQuery
  (query [this]
    '[:app/title :loading?])
  Object
  (render [this]
    (let [{:keys [app/title loading?]} (om/props this)]
      (dom/div nil
               (dom/p nil title)
               (dom/p nil (pr-str loading?))
               (dom/input #js {:ref :title})
               (let [click-handler (fn [e] (let [new-title (.-value (dom/node this :title))]
                                             (om/transact! this `[(app/update-title {:new-title ~new-title})
                                                                  (app/loading?)
                                                                  :app/title
                                                                  :loading?
                                                                  ])))]
                 (dom/div {}
                          (fancy-button-with-class {:onClick click-handler} "update")
                          (fancy-button-with-function-call {:onClick click-handler} "update")
                          (js/React.createElement fancy-button-with-function-as-component #js {:onClick click-handler} "update")
                          (fancy-button-with-function-as-component-factory #js {:onClick click-handler} "update")))))))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
    {:state (atom {})
     :normalize true
     :merge-tree (fn [a b] (println "|merge" a b) (merge a b))
     :parser parser
     :send (util/transit-post "/api")}))

(om/add-root! reconciler Root (gdom/getElement "app"))
