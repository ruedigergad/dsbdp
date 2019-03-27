;;;
;;;   Copyright 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Functionality for parallelized processing."}
  dsbdp.parallel-processing
  (:require
    (clojure.core [reducers :as reducers])
    [dsbdp.local-data-processing-pipeline :refer :all]))

(defn create-simple-pmap-processor
  [proc-fn batch-size out-fn]
  (let [batch (atom [])
        in-fn (fn [data]
                (swap! batch conj data)
                (when (>= (count @batch) batch-size)
                  (doseq [out-element (pmap proc-fn @batch)]
                    (out-fn out-element))
                  (reset! batch [])))]
    in-fn))

(defn create-simple-reducers-map-processor
  [proc-fn batch-size partition-size out-fn]
  (let [batch (atom [])
        in-fn (fn [data]
                (swap! batch conj data)
                (when (>= (count @batch) batch-size)
                  (doseq [out-element (reducers/fold
                                         partition-size
                                         reducers/cat
                                         reducers/append!
                                         (reducers/map proc-fn @batch))]
                    (out-fn out-element))
                  (reset! batch [])))]
    in-fn))

