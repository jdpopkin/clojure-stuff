(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [camel-snake-kebab.core :as kebab]
            [clojure.java.jdbc :as db]
            [clojure.data.json :as json]
            [clojure-getting-started.stories :as stories]
            [clj-http.client :as client]))

(def sample (env :sample "sample-string-thing"))
(def key (env :riot-key (slurp (io/resource ".env/riot"))))
(def na-url (str "https://na.api.pvp.net/api/lol/na/v2.5/league/challenger?type=RANKED_SOLO_5x5&api_key=" key))
(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "///yoloq"
              :user "yoloq"})

(defn fetch-challenger []
  (client/get na-url {:as :json}))

(defn add-index [coll]
  "Add an :index to each item in the collection which is its
  index in the collection."
  (reduce #(conj %1 (assoc %2 :index (count %1))) [] coll))

(defn prettify-challengers [challengers]
  (add-index (sort-by :leaguePoints (:entries (json/read-str (:body challengers) :key-fn keyword)))))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (concat (for [kind ["camel" "snake" "kebab"]]
                   (format "<a href=\"/%s?input=%s\">%s %s</a><br />"
                           kind sample kind sample))
                 ["<hr /><ul>"]
                 (for [s (db/query (env :database-url "postgres:///yoloq")
                                   ["select content from sayings"])]
                   (format "<li>%s</li>" (:content s)))
                 ["</ul>"])})

(defn record [input]
  (db/insert! (env :database-url "postgres:///yoloq")
              :sayings {:content input}))

(defroutes app
  (GET "/camel" {{input :input} :params}
       (record input)
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (kebab/->CamelCase input)})
  (GET "/snake" {{input :input} :params}
       (record input)
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (kebab/->snake_case input)})
  (GET "/kebab" {{input :input} :params}
       (record input)
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (kebab/->kebab-case input)})
  (GET "/" []
       (splash))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
