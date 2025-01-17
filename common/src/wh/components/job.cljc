(ns wh.components.job
  (:require
    #?(:clj [wh.pages.util :refer [html->hiccup]])
    [wh.common.job :as jobc]
    [wh.components.common :refer [wrap-img link img]]
    [wh.components.icons :refer [icon]]
    [wh.components.issue :refer [level->str level->icon]]
    [wh.interop :as interop]
    [wh.re-frame.events :refer [dispatch]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util]
    [wh.verticals :as verticals]))

(defn job-card
  [{:keys [company tagline title display-salary remuneration remote sponsorship-offered display-location role-type tags slug id published]
    :or   {published true}
    :as   job}
   {:keys [public? liked? user-has-applied? user-is-owner?]
    :or   {public?           true
           liked?            false
           user-has-applied? false
           user-is-owner?    false}}]
  (let [{company-name :name logo :logo} company
        skeleton? (and job (empty? (dissoc job :id :slug)))
        salary    (or display-salary (jobc/format-job-remuneration remuneration))
        job-tags  (if skeleton?
                    (map (fn [_i] (apply str (repeat (+ 8 (rand-int 30)) " "))) (range 6))
                    tags)]
    [:div {:class (util/merge-classes "card"
                                      "card--job"
                                      (str "card-border-color-" (rand-int 9))
                                      (str "i-cur-" (rand-int 9))
                                      (when public? "job-card--public")
                                      (when skeleton? "job-card--skeleton"))}
     [:span
      (when (not public?)
        [icon "like"
         :class (util/merge-classes "job__icon" "like" (when liked? "selected"))
         :on-click #(dispatch [:wh.events/toggle-job-like job])])
      [:a {:href (routes/path :job :params {:slug slug})}
       [:div.info
        [:div.logo
         (if (or skeleton? (not logo) public?)
           [:div]
           (wrap-img img logo {:alt (str company-name " logo") :w 48 :h 48}))]
        [:div.basic-info
         [:div.job-title title]
         (when (not public?)
           [:div.company-name company-name])
         [:div.location display-location]
         (cond-> [:div.card__perks]
                 remote                       (conj [icon "job-remote" :class "job__icon--small"] "Remote")
                 (not= role-type "Full time") (conj [icon "profile" :class "job__icon--small"] role-type)
                 sponsorship-offered          (conj [icon "job-sponsorship" :class "job__icon--small"] "Sponsorship"))
         [:div.salary salary]]]]]
     (into [:ul.tags.tags__job]
           (map (fn [tag] [:li [:a {:href (routes/path :pre-set-search :params {:tag (slug/slug tag)})} tag]])
                job-tags))
     [:div.tagline tagline]
     [:div.apply
      [:div.buttons
       [:a.button.button--inverted {:href (routes/path :job :params {:slug slug})}
        (if user-is-owner? "View" "More Info")]
       (when (not user-is-owner?)
         [:button.button (if public?
                           (interop/on-click-fn
                             (interop/show-auth-popup :jobcard-apply [:job :params {:slug slug} :query-params {:apply true}])) ;; TODO upate with slug
                           {:href (routes/path :job :params {:slug slug} :query-params {:apply true})})
          (if user-has-applied?
            "1-Click Apply"
            "Easy Apply")])]]
     (when (not published)
       [:div.card__label.card__label--unpublished.card__label--job
        "Unpublished"])]))

(defn company-header [{:wh.jobs.job.db/keys [title remote location]}]
  [:section.is-flex.job__company-header
   [:div.job__company-header__info
    [:h1 title]
    [:h3 (if remote
           [:div "Remote 🙌"]
           (jobc/format-job-location location remote))]]])

(defn highlight
  [title icon-name body]
  [:div.job__highlight
   [:div.job__highlight__content
    [:h2 title]
    body]
   [:div.job__highlight_icon
    (when title [icon icon-name])]])

(defn job-highlights [{:wh.jobs.job.db/keys [remuneration role-type sponsorship-offered remote tags benefits] :as _job}]
  (let [salary (jobc/format-job-remuneration remuneration)]
    [:section.job__job-highlights
     [highlight
      (when salary "Salary") "job-money"
      [:div.job__salary
       salary]]
     ;;
     [highlight
      (when role-type "Contract type") "job-contract"
      [:div.job__contract-type
       [:div role-type]
       (when sponsorship-offered [:div "Sponsorship offered"])
       (when remote [:div "Remote working"])]]
     ;;
     [highlight
      (when tags "Technologies & frameworks") "job-tech"
      [:div.job__technology
       [:div.row.summary__row__with-tags
        (let [skeleton? (nil? tags)
              tags (if skeleton? (map #(hash-map :key % :tag (apply str (repeat (+ 4 (rand-int 20)) " "))) (range 6)) tags)]
          (into [:ul.tags.tags--inline]
                (map (fn [tag]
                       [:li {:class (when skeleton? "tag--skeleton")
                             :key   (or (:key tag) tag)}
                        (or (:tag tag) tag)])
                     tags)))]]]
     ;;
     (when benefits
       [highlight
        "Benefits & perks" "job-benefits"
        [:div.job__benefits
         [:div.row
          (into [:ul]
                (for [item benefits]
                  [:li {:key item} item]))]]])]))

(defn information [{:wh.jobs.job.db/keys [description-html location-description] :as _job}]
  #?(:clj
     [:section.job__information
      [:div.job__job-description
       [:h2 (when description-html "Role overview")]
       [:div {:class "job__content job__content--expanded"}
        (html->hiccup description-html)]]
      (when location-description
        [:div.job__location
         [:h2 "Location"]
         [:div {:class "job__content job__content--expanded"}
          (html->hiccup location-description)]])]))




(defn apply-button [element-id {:wh.jobs.job.db/keys [id slug] :as _job}]
  [:div
   [:button.button.button--medium
    (merge {:id (str "job-view__logged-out-apply-button" (when element-id (str "__" element-id)))}
           (interop/on-click-fn
             (interop/show-auth-popup :jobpage-apply
                                      [:job
                                       :params {:slug slug}
                                       :query-params {:apply "true"}])))
    "Easy Apply"]
   [:button.button.button--medium.button--inverted
    (merge {:id (str "job-view__see-more-button" (when id (str "__" id)))}
           (interop/on-click-fn
             (interop/show-auth-popup :jobpage-apply
                                      [:job
                                       :params {:slug slug}])))
    "See More"]])

(defn candidate-action [job]
  [:section.job__candidate-action
   [apply-button "candidate-action-box" job]])

(defn lower-cta [vertical job]
  [:section.is-flex.job__lower-cta.is-hidden-mobile
   [:div.is-flex
    [icon "codi"]
    [:span (str "Engineers who find a new job through " (verticals/config vertical :platform-name) " average a 15% increase in salary.")]]
   [apply-button "2-engineers-who-find-new-job-through-site" job]])

(defn other-roles [jobs]
  [:div.job__other-roles
   [:h2 "Other roles that might interest you"]
   [:div.columns
    (doall (for [job jobs]
             ^{:key (:id job)}
             [:div.column [job-card job {:public? true}]]))]])

(defn apply-sticky [{:wh.jobs.job.db/keys [title slug] :as _job}]
  [:div.job__apply-sticky.is-hidden-desktop.is-flex.sticky
   {:id "job__apply-sticky"}
   [:div.job__apply-sticky__logo
    [icon "codi"]]
   [:div.job__apply-sticky__title title]
   [:button.button
    (merge {:id "job-apply-sticky__apply-button"}
           (interop/on-click-fn
             (interop/show-auth-popup :jobpage-apply
                                      [:job
                                       :params {:slug slug}])))
    "Easy Apply"]])

(defn issue
  [{:keys [id title repo level]}]
  [:div.issue
   [link title :issue :id id :class "title"]
   [:div.level
    [icon (level->icon level)] (str "Level: " (level->str level))]
   (when-let [language (:primary-language repo)]
     [:ul.tags.tags--inline
      [:li language]])])

(defn company-issues [{:wh.jobs.job.db/keys [company company-id] :as _job}]
  [:section.job__company-issues
   [:p.cta
    "Interested in seeing this company work? Contribute to their Open Source."]
   (for [item (:issues company)]
     ^{:key (:id item)}
     [issue item])
   [link "View All Issues" :issues-for-company-id :company-id company-id :class "button"]])

(defn page []
  (let [app-db @re-frame.db/app-db ; TODO: make it subscription-based
        vertical (<sub [:wh/vertical])
        job (:wh.jobs.job.db/sub-db app-db)
        tagline (:wh.jobs.job.db/tagline job)
        recommended-jobs (:wh.jobs.job.db/recommended-jobs job)
        error (:wh.jobs.job.db/error job)]
    [:div
     [:div.main-container
      (if (= :no-matching-job error)
        [:div.main
         [:h2 "Sorry! That job is either no longer live or never existed."]
         [:h3 "If you think this might be a mistake, double check the link, otherwise browse the available jobs on our "
          [link "Job board." :jobsboard :class "a--underlined"]]]
        [:div.main.job
         [:div.is-flex
          [:div.job__main
           [company-header job]
           [:div.is-hidden-desktop
            [candidate-action job]
            [job-highlights job]
            [company-issues job]]
           [:section.job__tagline
            [:h2 (when tagline "To sum it up...")]
            [:div tagline]]
           [information job]
           [lower-cta vertical job]]
          [:div.job__side.is-hidden-mobile
           [candidate-action job]
           [job-highlights job]
           [company-issues job]]]
         [other-roles recommended-jobs]])
      [apply-sticky job]
      [:script (interop/set-class-on-scroll "job__apply-sticky" "sticky--shown" 160)]]]))
