(ns wh.components.github
  (:require
    #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
    #?(:cljs [wh.subs :as subs :refer [<sub]])
    #?(:clj [wh.config :as config])))

(defn app-name []
  #?(:clj (config/get-in [:github :app :name]))
  #?(:cljs (<sub [::subs/github-app-name])))

(defn state-query-param []
  #?(:cljs (let [env (<sub [::subs/env])
                 pr-number (some-> (re-find #"-\d+" js/window.location.href)
                                   (subs 1))]
             (when (and (= :stage env) pr-number)
               (str "?state=" pr-number)))))

(defn install-gh-app-url []
  (str "https://github.com/apps/" (app-name) "/installations/new"
       (state-query-param)))

(defn install-github-app
  [{:keys [class label id]
    :or {label "Integrate with"}}]
  [:a.button.button--black.button--public.button--github
   (merge
     {:class class
      :href  (install-gh-app-url)}
     (when id
       {:id id}))
   [:span label] [:div]])