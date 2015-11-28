;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Main class for launching experiments"}
  dsbdp.main
  (:use
    clj-assorted-utils.util
    dsbdp.data-processing-dsl
    dsbdp.byte-array-conversion
    dsbdp.experiment-helper
    dsbdp.local-data-processing-pipeline)
  (:import
    (dsbdp Counter ProcessingLoop)
    (java.util HashMap Map))
  (:gen-class))

(defn -main [& args]
  (println "Starting dsbdp main...")
  (let [in-cntr (Counter.)
        out-cntr (Counter.)
        no-op true
        delta-cntr (delta-counter)
        stats-fn (fn []
                   (println
                     "in:" (long (/ (delta-cntr :in (.value in-cntr)) 1000)) "k;"
                     "out:" (long (/ (delta-cntr :out (.value out-cntr)) 1000)) "k;"))
        out-fn (fn [_ _]
                 (.inc out-cntr))
;        in-data 24N
        in-data (if (not no-op)
                  1)
        pipeline (if (not no-op)
                   (create-local-processing-pipeline
                     (create-no-op-proc-fns 2)
;                     (create-factorial-proc-fns 2)
                     out-fn))
        in-fn (if (not (nil? pipeline))
                (get-in-fn pipeline))
        in-loop (if (not (nil? in-fn))
                  (ProcessingLoop.
                    (fn []
                      (in-fn in-data)
                      (.inc in-cntr)))
                  (ProcessingLoop.
                    (fn []
                      (.inc in-cntr))))]
    (.start in-loop)
    (run-repeat (executor) stats-fn 1000)))

