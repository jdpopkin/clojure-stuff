(ns clojure-getting-started.stories)

(defn describe-player-change
  [player]
  (let [lp-verb (if (< (:league-points (:change player)) 0)
                  " lost "
                  " gained ")
        rank-phrase (cond
                     (< (:index (:change player)) 0) " climbed to "
                     (> (:index (:change player)) 0) " fell to "
                     :else " remained at ")]
    (str (:player-or-team-name (:new-item player)) lp-verb
         (Math/abs (:league-points (:change player))) "LP and"
         rank-phrase "rank "
         (inc (:index (:new-item player))) ".")))

(defn describe-section
  "Given one sorted section of the change-set, return a string describing
   the changes."
  [section]
  (clojure.string/join " " (map describe-player-change (:players section))))

(defn merge-on-id
  "Combine two vectors of player-league entries into a single
   vector of maps to old and new player-league entries."
  [old-list new-list]
  (map (fn [old-item]
         (let [new-item
               (first (filter #(= (:player-or-team-id old-item)
                                  (:player-or-team-id %)) new-list))]
           {:old-item old-item :new-item new-item})) old-list))

(defn find-change
  [old-item new-item]
  (let [diff-map-on-key
        (fn [key]
          (- (key new-item) (key old-item)))]
    {:league-points (diff-map-on-key :league-points)
     :index (diff-map-on-key :index)
     :name-changed? (not (= (:player-or-team-name old-item)
                            (:player-or-team-name new-item)))}))

(defn find-changes
  [merged-list]
  (map #(assoc % :change (find-change
                          (:old-item %)
                          (:new-item %))) merged-list))

(defn player-in-range?
  [player range]
  (let [new-rank (comp :index :new-item)
        old-rank (comp :index :old-item)]
    (and (or (< (new-rank player) (last range))
             (< (old-rank player) (last range)))
         (not (< (new-rank player) (first range)))
         (not (< (old-rank player) (first range))))))

(def partitioned-pairs
  (map #(identity {:name (str "Top " (last %))
          :bounds %})
       (partition 2 1 [-10 10 25 50 100 200])))
(defn partition-players
  [players]
  (map (fn [partition]
         (assoc partition :players
                (filter
                 #(player-in-range? % (:bounds partition))
                 players)))
   partitioned-pairs))

(defn rank-changes
  "Given vector of partitioned maps of players, order the players
   in this partition by the amount of LP they lost or gained."
  [partitions]
  (map (fn [partition]
         (assoc partition :players
                (sort-by #(Math/abs (:league-points (:change %)))
                         (:players partition))))
       partitions))

(defn describe-changes
  [old-list new-list]
  ; new list: combine lists on id? with reduce?
  (let [merged (merge-on-id old-list new-list)
        changed (find-changes merged)
        partitioned (partition-players changed)
        sorted (rank-changes partitioned)]
    (map describe-section sorted)))
