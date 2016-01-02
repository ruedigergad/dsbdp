;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Pipeline for processing data"}
  dsbdp.local-data-processing-pipeline
  (:require [dsbdp.data-processing-dsl :refer :all])
  (:require [clj-assorted-utils.util :refer :all])
  (:import
    (dsbdp Counter LocalTransferContainer ProcessingLoop)
    (java.util.concurrent ArrayBlockingQueue BlockingQueue LinkedBlockingQueue LinkedTransferQueue TimeUnit TransferQueue)))



(def ^Integer queue-size (int 512))
(def queue-setup
  (let [file-name "queue-setup.cfg"
        setup (if (file-exists? file-name)
                (do
                  (println "Using queue-setup defined in:" file-name)
                  (.trim ^String (slurp file-name)))
                (do
                  (println file-name "not found. Using default.")
;                  "ArrayBlockingQueue_put"))]
                  "ArrayBlockingQueue_put-counted-yield"))]
;                  "ArrayBlockingQueue_offer"))]
;                  "ArrayBlockingQueue_offer-counted-yield"))]
;                  "LinkedTransferQueue_transfer"))]
;                  "LinkedTransferQueue_transfer-counted-yield"))]
;                  "LinkedTransferQueue_tryTransfer-counted-1ms"))]
    (println "Using queue-setup:" setup)
    setup))

(defmacro create-queue
  []
  (println "Setting up queue creation for:" queue-setup)
  (let [expr (condp (fn [^String v ^String s] (.startsWith s v)) queue-setup
               "LinkedBlockingQueue" `(LinkedBlockingQueue. queue-size)
               "ArrayBlockingQueue" `(ArrayBlockingQueue. queue-size)
               "LinkedTransferQueue" `(LinkedTransferQueue.))]
    (println expr)
    expr))

(defmacro enqueue
  [queue data enqueued-counter dropped-counter]
  (println "Enqueueing data via:")
  (let [expr (condp (fn [^String v ^String s] (.endsWith s v)) queue-setup
               "put" `(.put ~queue ~data)
               "put-counted-yield" `(if (< (.size ~queue) queue-size)
                                      (do
                                        (.put ~queue ~data)
                                        (.inc ~enqueued-counter))
                                      (do
                                        (.inc ~dropped-counter)
                                        (Thread/yield)))
               "offer" `(.offer ~queue ~data)
               "offer-counted-yield" `(if (< (.size ~queue) queue-size)
                                        (do
                                          (.offer ~queue ~data)
                                          (.inc ~enqueued-counter))
                                        (do
                                          (.inc ~dropped-counter)
                                          (Thread/yield)))
               "transfer" `(.transfer ~queue ~data)
               "transfer-counted-no-sleep" `(if (.hasWaitingConsumer ~queue)
                                              (do
                                                (.transfer ~queue ~data)
                                                (.inc ~enqueued-counter))
                                              (.inc ~dropped-counter))
               "transfer-counted-sleep-1ms" `(if (.hasWaitingConsumer ~queue)
                                               (do
                                                 (.transfer ~queue ~data)
                                                 (.inc ~enqueued-counter))
                                               (do
                                                 (.inc ~dropped-counter)
                                                 (sleep 1)))
               "transfer-counted-yield" `(if (.hasWaitingConsumer ~queue)
                                               (do
                                                 (.transfer ~queue ~data)
                                                 (.inc ~enqueued-counter))
                                               (do
                                                 (.inc ~dropped-counter)
                                                 (Thread/yield)))
               "tryTransfer" `(.tryTransfer ~queue ~data)
               "tryTransfer-counted-no-timeout" `(if (.tryTransfer ~queue ~data)
                                                   (.inc ~enqueued-counter)
                                                   (.inc ~dropped-counter))
               "tryTransfer-counted-1ms" `(if (.tryTransfer ~queue ~data 1 TimeUnit/MILLISECONDS)
                                            (.inc ~enqueued-counter)
                                            (.inc ~dropped-counter))
               "tryTransfer-counted-10ms" `(if (.tryTransfer ~queue ~data 10 TimeUnit/MILLISECONDS)
                                             (.inc ~enqueued-counter)
                                             (.inc ~dropped-counter))
               "tryTransfer-counted-100ms" `(if (.tryTransfer ~queue ~data 100 TimeUnit/MILLISECONDS)
                                              (.inc ~enqueued-counter)
                                              (.inc ~dropped-counter)))]
    (println expr)
    expr))

(defmacro take-from-queue
  [queue]
  `(.take ~queue))



(defn create-local-processing-element
  ([^BlockingQueue in-queue f]
    (create-local-processing-element in-queue f nil))
  ([^BlockingQueue in-queue f id]
    (let [running (atom true)
          out-queue (create-queue)
          out-counter (Counter.)
          out-drop-counter (Counter.)
          proc-fn (fn []
                    (try
                      (let [^LocalTransferContainer c (take-from-queue in-queue)
                            new-out (f (.getIn c) (.getOut c))]
                        (if (not (nil? new-out))
                          (.setOut c new-out))
                        (enqueue out-queue c out-counter out-drop-counter))
                      (catch InterruptedException e
                        (if @running
                          (throw e)))))
          thread-name (if (not (nil? id))
                        (str "ProcessingElement_" id))
          proc-loop (if (not (nil? thread-name))
                      (ProcessingLoop.
                        thread-name
                        proc-fn)
                      (ProcessingLoop.
                        proc-fn))]
      (.start proc-loop)
      {:get-counts-fn (fn []
                        {:out (.value out-counter)
                         :dropped (.value out-drop-counter)})
       :interrupt (fn []
                    (reset! running false)
                    (.interrupt proc-loop))
       :out-queue out-queue
       :id id
       :thread-name thread-name})))

(defn interrupt
  [obj]
  ((obj :interrupt)))

(defn get-out-queue
  [obj]
  (obj :out-queue))

(defn get-id
  [obj]
  (obj :id))

(defn get-thread-name
  [obj]
  (obj :thread-name))

(defn get-counts
  [obj]
  ((obj :get-counts-fn)))

(defn create-local-processing-pipeline
  [fns out-fn]
  (let [running (atom true)
        in-queue (create-queue)
        proc-elements (reduce
                        (fn [v f]
                          (conj v (create-local-processing-element (get-out-queue (last v)) f (count v))))
                        [(create-local-processing-element in-queue (first fns) 0)]
                        (rest fns))
        ^BlockingQueue out-queue (get-out-queue (last proc-elements))
        out-proc-loop (ProcessingLoop.
                        "PipelineOut"
                        (fn []
                          (try
                            (let [^LocalTransferContainer c (take-from-queue out-queue)]
                              (if (not (nil? c))
                                (out-fn (.getIn c) (.getOut c))
                                (out-fn nil nil)))
                            (catch InterruptedException e
                              (if @running
                                (throw e))))))
        in-counter (Counter.)
        in-drop-counter (Counter.)]
    (.start out-proc-loop)
    {:get-counts-fn (fn []
                      (reduce
                        (fn [m pe]
                          (assoc m (get-id pe) (get-counts pe)))
                        {}
                        proc-elements))
     :interrupt (fn []
                  (reset! running false)
                  (doseq [pe proc-elements]
                    (interrupt pe))
                  (.interrupt out-proc-loop))
     :in-fn (fn [in]
              (enqueue in-queue (LocalTransferContainer. in nil) in-counter in-drop-counter))}))

(defn get-in-fn
  [obj]
  (obj :in-fn))

