(ns om-starter.server
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response file-response resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [om-starter.middleware :refer [wrap-transit-response wrap-transit-params]]
            [om-starter.parser :as parser]
            [om.next.server :as om]
            [bidi.bidi :as bidi]))

(def routes
  ["" {"/" :index
       "/the-list" :the-list
       ["/item/" :name] :item}])

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn index [req]
  (assoc (resource-response (str "html/index.html") {:root "public"})
         :headers {"Content-Type" "text/html"}))

(def the-list [{:item/name "A"
                :item/another-basic-key 1}
               {:item/name "B"
                :item/another-basic-key 2}
               {:item/name "C"
                :item/another-basic-key 3}])

(def details {"A" {:item/name "A"
                   :item/details 11}
              "B" {:item/name "B"
                   :item/details 12}
              "C" {:item/name "C"
                   :item/details 13}})

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index nil
      :the-list (generate-response the-list)
      :item (generate-response (get details (-> match :route-params :name)))
      nil)))

(def app
  (-> handler
      (wrap-resource "public")
      wrap-reload
      wrap-transit-response
      wrap-transit-params))
