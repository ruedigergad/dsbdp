;;;
;;;   Copyright 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Functions for self-adaptive local data processing pipelines."}
  dsbdp.local-dpp-self-adaptivity
  (:require
    [clj-assorted-utils.util :refer :all]
    [clojure.pprint :refer :all]
    [dsbdp.local-data-processing-pipeline :refer :all]
    [dsbdp.processing-fn-utils :refer :all]))

(defn create-stat-delta-counter
  "Create a delta counter for calculating the differences between subsequent measurements of pipeline statistics.
   Returns a function with one argument that takes the stats map as emitted from the pipeline.
   The returned function calculates the difference between the values of the passed stats map between subsequent invocations."
  [n]
  (let [delta-cntr (delta-counter)]
    (doseq [e (reduce
                (fn [m n]
                  (assoc m n {:out 0, :dropped 0}))
                {:pipeline {:in 0, :dropped 0}}
                (range 0 n))]
      (let [k (key e)
            v (val e)]
        (if (= :pipeline k)
          (delta-cntr (keyword (str k "-in")) (:in v))
          (delta-cntr (keyword (str k "-out")) (:out v)))
        (delta-cntr (keyword (str k "-dropped")) (:dropped v))))
    (fn [current-stats]
      (reduce
        (fn [m e]
          (let [k (key e)
                v (val e)]
            (assoc m k
                   (if (= :pipeline k)
                     {:in-delta (delta-cntr (keyword (str k "-in")) (:in v))
                      :dropped-delta (delta-cntr (keyword (str k "-dropped")) (:dropped v))}
                     {:out-delta (delta-cntr (keyword (str k "-out")) (:out v))
                      :dropped-delta (delta-cntr (keyword (str k "-dropped")) (:dropped v))}))))
        {}
        current-stats))))

(defn create-repetition-detector
  "Create a detector for detecting repetitions of pred-fn evaluating to true.
   Returns a function with one argument that takes a predicate function, pred-fn, as argument.
   The returned function only evaluates to true if pred-fn was true for the number of invocations defined by repetitions."
  [repetitions]
  (let [cntr (counter)]
    (fn [pred-f]
      (if (>= (cntr) repetitions)
        (cntr (fn [_] 0)))
      (if (pred-f)
        (cntr inc)
        (cntr (fn [_] 0)))
      (= repetitions (cntr)))))

(defn create-moving-average-calculator
  "Create a calculator for calculating a moving average of cnt instances.
   Returns a single argument function that takes a numerical value as argument.
   The returned function evaluates to the moving average of the last cnt instances of numerical values that were passed."
  [cnt]
  (let [data (ref (vec (repeat cnt 0)))]
    (fn
      ([]
        (/ (apply + @data) cnt))
      ([value]
        (dosync (alter data (fn [d v] (-> d (subvec 1) (conj v))) value))))))

(defn create-drop-detector
  "Create a detector for detecting drops based on pipeline stats delta data.
   n-repetitions define the number of subsequent repetitions of drops that have to occur in order to trigger a detection.
   n-detectors defines the number of detectors that are to be used.
   threshold defines the threshold of from which value on a drop is considered.
   Returns a single argument function that serves as the actual detector.
   The returned function takes the pipeline stats delta as argument and returns a vector of boolean values that indicate if drops were detected for a given pipeline stage."
  [n-repetitions n-detectors threshold]
  (let [rep-detectors (reduce
                        (fn [v _] (conj v (create-repetition-detector n-repetitions)))
                        []
                        (repeat n-detectors 0))]
    (fn [deltas]
      (reduce
        (fn [v e]
          (conj v ((rep-detectors (inc (key e))) #(> (:dropped-delta (val e)) threshold))))
        [((rep-detectors 0) #(> (get-in deltas [:pipeline :dropped-delta]) threshold))]
        (sort (dissoc deltas :pipeline))))))

(defn get-drop-indices
  "Get a vector of the indices for which drops were detected in the input vector drop-det-vec.
   More generically, returns a vector of the indices of the elements that were true in in the input vector."
  [drop-det-vec]
  (vec
    (reduce-kv
      (fn [o k v] (if v (conj o k) o))
      []
      drop-det-vec)))

(defn get-non-drop-indices
  "Get a vector of the indices for which no drops were detected in the input vector drop-det-vec.
   More generically, returns a vector of the indices of the elements that were not true in the input vector."
  [drop-det-vec]
  (vec
    (reduce-kv
      (fn [o k v] (if (not v) (conj o k) o))
      []
      drop-det-vec)))

(defn update
  "Update the element at the given index of the vector v by applying the function f to it."
  [v idx f]
  (assoc v idx (f (v idx))))

(defn create-mapping-updater
  "Create an updater for updating function mappings based on detected drops.
   Returns a function that takes a function mapping and a vector of boolean values as returned from a drop detector as input and which calculates a new function mapping with the aim of avoiding drops."
  []
  (let [limit-reached (atom (sorted-set))]
    (fn [orig-mapping drop-det-vec]
      (let [fn-drops (into [] (subvec drop-det-vec 0 (- (count drop-det-vec) 1)))
            drop-indices (get-drop-indices fn-drops)
            last-drop (last drop-indices)
            non-drop-indices (get-non-drop-indices fn-drops)
            last-non-drop (last
                            (filter
                              #(not (@limit-reached %))
                              non-drop-indices))]
        (cond
          (every? #(= false %) fn-drops)
            orig-mapping
          (nil? last-non-drop)
            orig-mapping
          (>= last-drop last-non-drop)
            (let [decremented-mapping (update orig-mapping last-drop dec)]
              (if
                (=
                  (inc last-drop)
                  (-
                    (count fn-drops)
                    (count @limit-reached)))
                (swap! limit-reached conj last-drop))
              (update decremented-mapping last-non-drop inc))
          (< last-drop last-non-drop)
            (let [delta (- last-non-drop last-drop)
                  drop-sum (-
                             (reduce
                               (fn [s idx]
                                 (+ s (orig-mapping idx)))
                               0
                               drop-indices)
                             (count drop-indices))
                  available (min delta drop-sum)
                  incremented-mapping (loop [av available
                                             idx (dec (count non-drop-indices))
                                             m orig-mapping]
                                        (if (> av 0)
                                          (recur
                                            (dec av)
                                            (if (< (dec idx) 0)
                                              (dec (count non-drop-indices))
                                              (dec idx))
                                            (update m (non-drop-indices idx) inc))
                                          m))
                  ]
              (loop [av available
                     idx (dec (count drop-indices))
                     m incremented-mapping]
                (if (> av 0)
                  (recur
                    (if (> (orig-mapping (drop-indices idx)) 1)
                      (dec av)
                      av)
                    (if (< (dec idx) 0)
                      (dec (count drop-indices))
                      (dec idx))
                    (if (> (orig-mapping (drop-indices idx)) 1)
                      (update m (drop-indices idx) dec)
                      m))
                  m)))
          :default nil
          )))))

(defn create-self-adaptivity-controller
  "Create a controller for a self-adaptive data processing pipeline."
  [cfg pipeline orig-fns mapping]
  (let [inactivity (cfg :inactivity)
        inactivity-counter (counter inactivity)
        stats-delta-counter (create-stat-delta-counter (count @mapping))
        drop-detector (create-drop-detector
                        (cfg :repetition)
                        (inc (count @mapping))
                        (cfg :threshold))
        mapping-updater (create-mapping-updater)]
    (add-watch
      mapping
      nil
      (fn [_ r old-state new-state]
        (println "Mapping changed from" old-state "to" new-state)
        (set-proc-fns-vec pipeline (combine-proc-fns-vec new-state orig-fns))))
    (fn [stats]
      (let [stats-delta (stats-delta-counter stats)]
        (if (< (inactivity-counter) inactivity)
          (inactivity-counter inc)
          (let [calculated-mapping (mapping-updater @mapping (drop-detector stats-delta))]
            (inactivity-counter (fn [_] 0))
            (when (not= @mapping calculated-mapping)
              (reset! mapping calculated-mapping))))))))

(defn update-stats
  [obj stats]
  (obj stats))

