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
  (:require
    (clj-assorted-utils [util :refer :all])
    (clojure.core [async :as async])
    (clojure.core [reducers :as reducers])
    (clojure.tools [cli :refer :all])
    (dsbdp
      [data-processing-dsl :refer :all]
      [byte-array-conversion :refer :all]
      [experiment-helper :refer :all]
      [local-data-processing-pipeline :refer :all]
      [local-dpp-self-adaptivity :refer :all]
      [parallel-processing :refer :all]
      [processing-fn-utils :as utils]))
  (:import
    (dsbdp Counter ExperimentHelper LatencyProbe LatencyProbeCollector ProcessingLoop)
    (java.lang.management ManagementFactory ThreadInfo ThreadMXBean)
    (java.util HashMap Map))
  (:gen-class))

(defn create-thread-info-fn
  []
  (let [^ThreadMXBean tmxb (ManagementFactory/getThreadMXBean)
        cpu-time-supported (.isThreadCpuTimeSupported tmxb)
        delta-cntr (delta-counter)]
    (.setThreadContentionMonitoringEnabled tmxb true)
    (if cpu-time-supported
      (.setThreadCpuTimeEnabled tmxb true))
    (fn []
      (let [t-ids (sort (vec (.getAllThreadIds tmxb)))]
        (doseq [t-id t-ids]
          (let [^ThreadInfo t-info (.getThreadInfo tmxb ^long t-id)
                t-name (.getThreadName t-info)
                cpu-time (if cpu-time-supported
                           (double (/ (.getThreadCpuTime tmxb t-id) 1000000000.0))
                           -1)
                user-time (if cpu-time-supported
                            (double (/ (.getThreadUserTime tmxb t-id) 1000000000.0))
                            -1)
                waited (.getWaitedTime t-info)
                blocked (.getBlockedTime t-info)]
            (println (str t-id "," t-name "," cpu-time "," user-time "," waited "," blocked ","
                       (delta-cntr (str "cpu-" t-id) cpu-time) ","
                       (delta-cntr (str "user-" t-id) user-time) ","
                       (delta-cntr (str "waited-" t-id) waited) ","
                       (delta-cntr (str "blocked-" t-id) blocked)))))))))

(def cli-options
  [["-b" "--batch-size BATCH-SIZE"
    "The number of data instances to be generated for one batch."
    :default 2000
    :parse-fn #(Integer/parseInt %)]
   ["-c" "--collection-size COLLECTION-SIZE"
    "The size of the collection that is to be processed with pmap and reducers."
    :default 512
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--batch-delay BATCH-DELAY"
    "The delay in ms between generating batches."
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-h" "--help"]
   ["-i" "--in-data IN-DATA"
    "The input data to be used."
    :default nil
    :parse-fn #(binding [*read-eval* false] (read-string %))]
   ["-l" "--pipeline-length PIPELINE-LENGTH"
    :default 2
    :parse-fn #(Integer/parseInt %)]
   ["-L" "--latency"
    "Measure the latency that is introduced by the data processing."]
   ["-m" "--fn-mapping FN-MAPPING"
    "The mapping of dsl-expressions to processing functions."
    :default [5 5 4 3]
    :parse-fn #(binding [*read-eval* false] (read-string %))]
   ["-p" "--partition-size PARTITION-SIZE"
    "The size of the partitions that are used for the reducer-based processing."
    :default 128
    :parse-fn #(Integer/parseInt %)]
   ["-s" "--scenario SCENARIO"
    "The scenario that is to be used."
    :default "no-op"]
   ["-S" "--self-adaptivity-cfg SELF-ADAPTIVITY-CFG"
    "Configuration for self-adaptive adjustment of the data processing pipeline."
    :default nil
    :parse-fn #(binding [*read-eval* false] (read-string %))]
   ])

(defn create-direct-proc-fn
  [scenario]
  (condp (fn [^String v ^String s] (.startsWith s v)) scenario
    "busy-sleep" (fn [in] (ExperimentHelper/busySleep ^long (first in)))
    "factorial" factorial
    "pcap-clj-map" (create-proc-fn sample-pcap-processing-definition-clj-map)
    "pcap-java-map" (create-proc-fn sample-pcap-processing-definition-java-map)
    "pcap-json" (create-proc-fn sample-pcap-processing-definition-json)
    "pcap-csv" (create-proc-fn sample-pcap-processing-definition-csv)
    "no-op" (fn [_] 1)
    nil))

(defn prepare-in-data
  [options]
  (let [scenario (:scenario options)
        in-data-arg (:in-data options)
        in-data (if (not (nil? in-data-arg))
                  in-data-arg
                  (condp (fn [^String v ^String s] (.startsWith s v)) scenario
                    "no-op" 1
                    "busy-sleep" [100000 100000 100000 100000]
                    "factorial" 300N
                    "pcap" pcap-byte-array-test-data
                    "self-adaptive" 1
                    "nil" nil))]
    (println "in-data:" in-data)
    in-data))

(defn prepare-proc-fns
  [scenario pipeline-length fn-mapping in-data options]
  (let [fns (condp = scenario
              "no-op" (create-no-op-proc-fns pipeline-length)
              "busy-sleep" (create-busy-sleep-proc-fns (count in-data))
              "factorial" [(fn [i _] (factorial i))]
              "factorial-inc" (create-factorial-proc-fns pipeline-length)
              "pcap-clj-map" (let [pcap-fn (create-proc-fn sample-pcap-processing-definition-clj-map)]
                               [(fn [i _] (pcap-fn i))])
              "pcap-clj-map-inc" (combine-proc-fns-vec
                                   @fn-mapping
                                   sample-pcap-processing-definition-clj-map)
              "pcap-java-map" (let [pcap-fn (create-proc-fn sample-pcap-processing-definition-java-map)]
                                [(fn [i _] (pcap-fn i))])
              "pcap-java-map-inc" (combine-proc-fns-vec
                                    @fn-mapping
                                    sample-pcap-processing-definition-java-map)
              "pcap-json" (let [pcap-fn (create-proc-fn sample-pcap-processing-definition-json)]
                            [(fn [i _] (pcap-fn i))])
              "pcap-json-inc" (combine-proc-fns-vec
                                @fn-mapping
                                sample-pcap-processing-definition-json)
              "pcap-csv" (let [pcap-fn (create-proc-fn sample-pcap-processing-definition-csv)]
                           [(fn [i _] (pcap-fn i))])
              "pcap-csv-inc" (combine-proc-fns-vec
                               @fn-mapping
                               sample-pcap-processing-definition-csv)
              "self-adaptive-low-throughput" (utils/combine-proc-fns-vec
                                               @fn-mapping
                                               synthetic-low-throughput-self-adaptivity-processing-fns)
              "self-adaptive-average-throughput" (utils/combine-proc-fns-vec
                                                   @fn-mapping
                                                   synthetic-average-throughput-self-adaptivity-processing-fns)
              "self-adaptive-high-throughput" (utils/combine-proc-fns-vec
                                                @fn-mapping
                                                synthetic-high-throughput-self-adaptivity-processing-fns)
              nil)]
    (println "Prepared fns for proc-fns:" fns)
    (atom
      (if (and (vector? fns) (:latency options))
        (do
          (println "Wrapping proc-fns for latency measurement.")
          (vec (map (fn [f] (fn [^LatencyProbe lp out] (f (.getData lp) out))) fns)))
        (do
          (println "Using fns vector as-is for proc-fns.")
          fns)))))

(defn prepare-in-fn
  [options scenario in-data pre-in-fn out-fn in-chan pipeline queue-size]
  (let [batch-delay (:batch-delay options)
        batch-size (:batch-size options)
        create-batched-in-fn (fn [in-fn]
                               (fn []
                                 (doseq [i (repeat batch-size 0)]
                                   (in-fn (pre-in-fn in-data)))
                                 (sleep batch-delay)))
        create-flood-in-fn (fn [in-fn]
                             (fn []
                               (in-fn (pre-in-fn in-data))))
        direct-proc-fn (let [f (create-direct-proc-fn scenario)]
                         (if (:latency options)
                           (do
                             (println "Wrapping direct processing fn for latency measurements.")
                             (fn [^LatencyProbe lp]
                               (f (.getData lp))
                               lp))
                           (do
                             (println "Using default direct processing fn.")
                             f)))
        collection-size (:collection-size options)
        partition-size (:partition-size options)
        in-fn (cond
                (.endsWith scenario "-direct")
                  (fn []
                    (out-fn
                      (direct-proc-fn
                        (pre-in-fn in-data))))
                (.endsWith scenario "-direct-pmap")
                  (let [d (repeat collection-size in-data)]
                    (doall d)
                    (fn []
                      (doseq [obj (pmap (fn [in-obj] (direct-proc-fn (pre-in-fn in-obj))) d)]
                        (out-fn obj))))
                (.endsWith scenario "-direct-reducers-map")
                  (let [in-vec (vec (repeat collection-size in-data))]
                    (doall in-vec)
                    (fn []
                      (doseq [obj (reducers/fold
                                    partition-size
                                    reducers/cat
                                    reducers/append!
                                    (reducers/map (fn [in-obj] (direct-proc-fn (pre-in-fn in-obj))) in-vec))]
                        (out-fn obj))))
                (.endsWith scenario "-simple-pmap")
                  (let [pmap-proc (create-simple-pmap-processor direct-proc-fn collection-size out-fn)]
                    (fn []
                      (pmap-proc (pre-in-fn in-data))))
                (.endsWith scenario "-simple-reducers-map")
                  (let [red-proc (create-simple-reducers-map-processor direct-proc-fn collection-size partition-size out-fn)]
                    (fn []
                      (red-proc (pre-in-fn in-data))))
                (.endsWith scenario "-async-pipeline")
                  (if
                    (and
                      (not (nil? in-data))
                      (> batch-delay 0)
                      (> batch-size 0))
                    (create-batched-in-fn
                      (fn [data]
                        (async/>!! in-chan data)))
                    (if
                      (not (nil? in-data))
                      (create-flood-in-fn
                        (fn [data]
                          (async/>!! in-chan data)))))
                (and
                  (not (nil? in-data))
                  (not (nil? pipeline))
                  (> batch-delay 0)
                  (> batch-size 0)) (create-batched-in-fn (get-in-fn pipeline))
                (and
                  (not (nil? in-data))
                  (not (nil? pipeline))) (create-flood-in-fn (get-in-fn pipeline))
                :default (if (not (.contains scenario "async-pipeline"))
                           out-fn))]
    in-fn))

(defn -main [& args]
  (println "Starting dsbdp main...")
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (when (:help options)
      (println summary)
      (System/exit 0))
    (println "Using options:" options)
    (println "Using args:" arguments)
    (let [in-cntr (Counter.)
          pre-in-fn (if (:latency options)
                      (do
                        (println "Creating latency measurement pre-in-fn.")
                        (fn [in-obj]
                          (.inc in-cntr)
                          (LatencyProbe. in-obj)))
                      (do
                        (println "Creating default measurement pre-in-fn.")
                        (fn [in-obj]
                          (.inc in-cntr)
                          in-obj)))
          out-cntr (Counter.)
          latency-probe-collector (LatencyProbeCollector.)
          out-fn (if (:latency options)
                   (do
                     (println "Creating latency measurement out-fn.")
                     (fn [^LatencyProbe lp]
                       (.done lp)
                       (.addProbe latency-probe-collector lp)
                       (.inc out-cntr)))
                   (do
                     (println "Creating default measurement out-fn.")
                     (fn [_] (.inc out-cntr))))
          delta-cntr (delta-counter)
          ^String scenario (:scenario options)
          in-data (prepare-in-data options)
          fn-mapping (atom (:fn-mapping options))
          _ (println "fn-mapping:" @fn-mapping)
          pipeline-length (:pipeline-length options)
          proc-fns (prepare-proc-fns scenario pipeline-length fn-mapping in-data options)
          pipeline (if
                     (and
                       (not (nil? in-data))
                       (not (.endsWith scenario "-direct"))
                       (not (.endsWith scenario "-direct-pmap"))
                       (not (.endsWith scenario "-direct-reducers-map"))
                       (not (.contains scenario "async-pipeline")))
                     (create-local-processing-pipeline
                       @proc-fns
                       (fn [in _]
                         (out-fn in))))
          in-chan (if (.contains scenario "async-pipeline")
                    (async/chan queue-size))
          out-chan (if (.contains scenario "async-pipeline")
                     (async/chan queue-size))
          in-fn (prepare-in-fn options scenario in-data pre-in-fn out-fn in-chan pipeline queue-size)
          in-loop (ProcessingLoop.
                    "DataGenerationLoop"
                    in-fn)
          async-out-count-loop (if
                                 (and
                                   (.endsWith scenario "-async-pipeline")
                                   (not (nil? in-data)))
                                 (ProcessingLoop.
                                   "AsyncOutputCountLoop"
                                   (fn []
                                     (out-fn (async/<!! out-chan)))))
          async-count-go (when
                           (and
                             (.endsWith scenario "-async-pipeline-go")
                             (not (nil? in-data)))
                           (println "Starting async-pipeline-go counting.")
                           (async/go
                             (loop []
                               (out-fn (async/<! out-chan))
                               (recur))))
          direct-proc-fn (let [f (create-direct-proc-fn scenario)]
                           (if (:latency options)
                             (do
                               (println "Wrapping async-pipeline direct processing fn for latency measurements.")
                               (fn [^LatencyProbe lp]
                                 (f (.getData lp))
                                 lp))
                             (do
                               (println "Using default async-pipeline direct processing fn.")
                               f)))
          async-pipeline (if
                           (and
                             (.contains scenario "-async-pipeline")
                             (not (nil? in-data)))
                           (async/pipeline
                             pipeline-length
                             out-chan
                             (map
                               (fn [in-data]
                                 (let [out-data (direct-proc-fn in-data)]
                                   (if (nil? out-data)
                                     1
                                     out-data))))
                             in-chan))
          self-adaptivity-cfg (options :self-adaptivity-cfg)
          self-adaptivity-controller (if (not (nil? self-adaptivity-cfg))
                                       (create-self-adaptivity-controller
                                         self-adaptivity-cfg
                                         pipeline
                                         (condp = scenario
                                           "self-adaptive-low-throughput" synthetic-low-throughput-self-adaptivity-processing-fns
                                           "self-adaptive-average-throughput" synthetic-average-throughput-self-adaptivity-processing-fns
                                           "self-adaptive-high-throughput" synthetic-high-throughput-self-adaptivity-processing-fns)
                                         fn-mapping))
          thread-info-fn (create-thread-info-fn)
          stats-fn (fn []
                     (let [in (double (/ (.value in-cntr) 1000.0))
                           out (double (/ (.value out-cntr) 1000.0))]
                       (println
                         "time-delta:" (delta-cntr :time (System/currentTimeMillis)) "ms;"
                         "in:" in "k;"
                         "out:" out "k;"
                         "in-delta:" (delta-cntr :in in) "k/s;"
                         "out-delta:" (delta-cntr :out out) "k/s;"
                         "latency:" (format "%.0f" (.getMean latency-probe-collector)))
                       (.reset latency-probe-collector)
                       (if (not (nil? pipeline))
                         (let [counts (get-counts pipeline)]
                           (println "mapping:" @fn-mapping)
                           (println counts)
                           (if (not (nil? self-adaptivity-controller))
                             (update-stats self-adaptivity-controller counts))))))]
      (println "Starting experiment...")
      (.setName (Thread/currentThread) "Main")
      (run-repeat
        (executor)
        (fn []
          (stats-fn)
          (thread-info-fn) (println)
          )
        1000)
      (run-once
        (executor)
        (fn [] (System/exit 0)) 120000)
      (if (.endsWith scenario "-async-pipeline-go")
        (do
          (println "Starting async-pipeline-go input creation.")
          (async/go
            (loop []
              (async/>! in-chan (pre-in-fn in-data))
              (recur)))
          (sleep 130000))
        (do
          (println "Starting in-loop.")
          (.start in-loop)))
      (if
        (not (nil? async-out-count-loop))
        (.start async-out-count-loop)))))

