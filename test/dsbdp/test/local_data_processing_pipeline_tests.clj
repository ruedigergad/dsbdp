;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for the data processing pipeline"}
  dsbdp.test.local-data-processing-pipeline-tests
  (:require
    [clj-assorted-utils.util :refer :all]
    [clojure.test :refer :all]
    [dsbdp.local-data-processing-pipeline :refer :all])
  (:import
    (dsbdp Counter LocalTransferContainer ProcessingLoop)
    (java.util.concurrent LinkedTransferQueue)))



(deftest simple-documentation-example-test-1
  (let [proc-fns [(fn [in out] (.toLowerCase in))
                  (fn [in out] (clojure.string/split out #"/"))
                  (fn [in out] (reverse out))
                  (fn [in out] (filter #(not (.isEmpty %)) out))
                  (fn [in out] (clojure.string/join "." out))]
        in-data "/com/Example/WWW"
        expected "www.example.com"
        out-flag (prepare-flag)
        out-data (atom nil)
        pipeline (create-local-processing-pipeline
                   proc-fns
                   (fn [in out]
                     (println "in:" in "; out:" out)
                     (reset! out-data out) (set-flag out-flag)))]
    ((get-in-fn pipeline) in-data)
    (await-flag out-flag)
    (is (= expected @out-data))
    (interrupt pipeline)))



(deftest local-transfer-container-constructor-test
  (let [container (LocalTransferContainer. "foo" "bar")]
    (is (= "foo" (.getIn container)))
    (is (= "bar" (.getOut container)))))

(deftest local-transfer-container-set-out-test
  (let [container (LocalTransferContainer. "foo" "bar")]
    (is (= "bar" (.getOut container)))
    (.setOut container "baz")
    (is (= "baz" (.getOut container)))))



(deftest create-local-processing-element-test
  (let [in-queue (LinkedTransferQueue.)
        flag (prepare-flag)
        proc-element (create-local-processing-element
                       in-queue
                       (fn [_ _] (set-flag flag) 0))]
    (.put in-queue (LocalTransferContainer. "in" "out"))
    (await-flag flag)
    (is (flag-set? flag))
    (interrupt proc-element)))

(deftest create-local-processing-element-id-test
  (let [in-queue (LinkedTransferQueue.)
        proc-element (create-local-processing-element
                       in-queue
                       (fn [_ _] 0)
                       123)]
    (is (= 123 (get-id proc-element)))
    (is (= "ProcessingElement_123" (get-thread-name proc-element)))
    (interrupt proc-element)))

(deftest create-local-processing-element-no-id-test
  (let [in-queue (LinkedTransferQueue.)
        proc-element (create-local-processing-element
                       in-queue
                       (fn [_ _] 0))]
    (is (nil? (get-id proc-element)))
    (is (nil? (get-thread-name proc-element)))
    (interrupt proc-element)))

(deftest simple-local-processing-element-in-args-test
  (let [in-queue (LinkedTransferQueue.)
        flag (prepare-flag)
        in-arg (atom nil)
        out-arg (atom nil)
        proc-element (create-local-processing-element
                       in-queue
                       (fn [in out]
                         (reset! in-arg in)
                         (reset! out-arg out)
                         (set-flag flag)
                         0))]
    (.put in-queue (LocalTransferContainer. "in" "out"))
    (await-flag flag)
    (is (= "in" @in-arg))
    (is (= "out" @out-arg))
    (interrupt proc-element)))

(deftest simple-local-processing-element-output-test
  (let [in-queue (LinkedTransferQueue.)
        flag (prepare-flag)
        tmp-flag (prepare-flag)
        out (atom nil)
        proc-element (create-local-processing-element
                       in-queue
                       (fn [in out]
                         "foobar"))]
    (doto
      (ProcessingLoop. (fn []
                         (set-flag tmp-flag)
                         (let [v (.take (get-out-queue proc-element))]
                           (reset! out v)
                           (set-flag flag))))
      (.start))
    (await-flag tmp-flag)
    (.put in-queue (LocalTransferContainer. "in" "out"))
    (await-flag flag)
    (is (= "in" (.getIn @out)))
    (is (= "foobar" (.getOut @out)))
    (let [cntr (get-counts proc-element)]
      (is (= 1 (:out cntr)))
      (is (= 0 (:dropped cntr))))
    (interrupt proc-element)))

(deftest simple-local-processing-element-chaining-test
  (let [in-queue (LinkedTransferQueue.)
        flag (prepare-flag)
        tmp-flag (prepare-flag)
        out (atom nil)
        proc-element-1 (create-local-processing-element
                         in-queue
                         (fn [in out]
                           (str in "foo")))
        proc-element-2 (create-local-processing-element
                         (get-out-queue proc-element-1)
                         (fn [in out]
                           (str out "bar")))]
    (doto
      (ProcessingLoop. (fn []
                         (set-flag tmp-flag)
                         (let [v (.take (get-out-queue proc-element-2))]
                           (reset! out v)
                           (set-flag flag))))
      (.start))
    (await-flag tmp-flag)
    (.put in-queue (LocalTransferContainer. "in" "out"))
    (await-flag flag)
    (is (= "in" (.getIn @out)))
    (is (= "infoobar" (.getOut @out)))
    (interrupt proc-element-1)
    (interrupt proc-element-2)))

(deftest simple-local-processing-element-counter-drop-test
  (let [in-queue (LinkedTransferQueue.)
        proc-element (create-local-processing-element
                       in-queue
                       (fn [_ _] 0))]
    (.put in-queue (LocalTransferContainer. "in" "out"))
    (.put in-queue (LocalTransferContainer. "in" "out"))
    (.put in-queue (LocalTransferContainer. "in" "out"))
    (sleep 100)
    (let [cntr (get-counts proc-element)]
      (is (= 0 (:out cntr)))
      (is (= 3 (:dropped cntr))))
    (interrupt proc-element)))

(deftest simple-local-processing-element-change-proc-fn-test
  (let [in-queue (LinkedTransferQueue.)
        flag-1 (prepare-flag)
        flag-2 (prepare-flag)
        flag-3 (prepare-flag)
        tmp-flag (prepare-flag)
        out (atom nil)
        proc-fn-1 (fn [in out]
                    (set-flag flag-1)
                    "foo")
        proc-fn-2 (fn [in out]
                    (set-flag flag-2)
                    "bar")
        proc-fn-3 (fn [in out]
                    (set-flag flag-3)
                    "baz")
        proc-element (create-local-processing-element
                       in-queue
                       proc-fn-1)]
    (doto
      (ProcessingLoop. (fn []
                         (set-flag tmp-flag)
                         (let [v (.take (get-out-queue proc-element))]
                           (reset! out v))))
      (.start))
    (await-flag tmp-flag)
    (.put in-queue (LocalTransferContainer. "in-1" "out-1"))
    (await-flag flag-1)
    (sleep 100)
    (is (= "in-1" (.getIn @out)))
    (is (= "foo" (.getOut @out)))
    (let [cntr (get-counts proc-element)]
      (is (= 1 (:out cntr)))
      (is (= 0 (:dropped cntr))))
    (set-proc-fn proc-element proc-fn-2)
    (.put in-queue (LocalTransferContainer. "in-2" "out-2"))
    (await-flag flag-2)
    (sleep 100)
    (is (= "in-2" (.getIn @out)))
    (is (= "bar" (.getOut @out)))
    (let [cntr (get-counts proc-element)]
      (is (= 2 (:out cntr)))
      (is (= 0 (:dropped cntr))))
    (set-proc-fn proc-element proc-fn-3)
    (.put in-queue (LocalTransferContainer. "in-3" "out-3"))
    (await-flag flag-3)
    (sleep 100)
    (is (= "in-3" (.getIn @out)))
    (is (= "baz" (.getOut @out)))
    (let [cntr (get-counts proc-element)]
      (is (= 3 (:out cntr)))
      (is (= 0 (:dropped cntr))))
    (interrupt proc-element)))

(deftest simple-local-pipeline-send-test
  (let [flag-proc (prepare-flag)
        flag-out (prepare-flag)
        proc-fns [(fn [_ _] (set-flag flag-proc) 0)]
        pipeline (create-local-processing-pipeline
                   proc-fns
                   (fn [_ _] (set-flag flag-out)))]
    ((get-in-fn pipeline) "foo")
    (await-flag flag-proc)
    (await-flag flag-out)
    (is (flag-set? flag-proc))
    (is (flag-set? flag-out))
    (interrupt pipeline)))

(deftest simple-three-staged-local-pipeline-send-test
  (let [flag-out (prepare-flag)
        out (atom nil)
        proc-fns [(fn [in _] (inc in))
                  (fn [_ out] (inc out))
                  (fn [_ out] (inc out))]
        pipeline (create-local-processing-pipeline
                   proc-fns
                   (fn [_ o] (reset! out o) (set-flag flag-out)))]
    ((get-in-fn pipeline) 0)
    (await-flag flag-out)
    (is (flag-set? flag-out))
    (is (= 3 @out))
    (interrupt pipeline)))

(deftest simple-pipeline-set-proc-fns-vec-test
  (let [flag-out (atom (prepare-flag))
        out (atom nil)
        proc-fns-1 [(fn [in _] (inc in))
                    (fn [_ out] (inc out))
                    (fn [_ out] (inc out))]
        proc-fns-2 [(fn [in _] (dec in))
                    (fn [_ out] (dec out))
                    (fn [_ out] (dec out))]
        pipeline (create-local-processing-pipeline
                   proc-fns-1
                   (fn [_ o] (reset! out o) (set-flag @flag-out)))]
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= 3 @out))
    (set-proc-fns-vec pipeline proc-fns-2)
    (reset! flag-out (prepare-flag))
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= -3 @out))
    (interrupt pipeline)))

(deftest simple-pipeline-increase-length-by-1-test
  (let [flag-out (atom (prepare-flag))
        out (atom nil)
        proc-fns-1 [(fn [in _] (inc in))
                    (fn [_ out] (inc out))
                    (fn [_ out] (inc out))]
        proc-fns-2 [(fn [in _] (dec in))
                    (fn [_ out] (dec out))
                    (fn [_ out] (dec out))
                    (fn [_ out] (dec out))]
        pipeline (create-local-processing-pipeline
                   proc-fns-1
                   (fn [_ o] (reset! out o) (set-flag @flag-out)))]
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= 3 @out))
    (set-proc-fns-vec pipeline proc-fns-2)
    (reset! flag-out (prepare-flag))
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= -4 @out))
    (interrupt pipeline)))

(deftest simple-pipeline-increase-length-by-n-test
  (let [flag-out (atom (prepare-flag))
        out (atom nil)
        proc-fns-1 [(fn [in _] (inc in))
                    (fn [_ out] (inc out))]
        proc-fns-2 [(fn [in _] (dec in))
                    (fn [_ out] (dec out))
                    (fn [_ out] (dec out))
                    (fn [_ out] (dec out))]
        pipeline (create-local-processing-pipeline
                   proc-fns-1
                   (fn [_ o] (reset! out o) (set-flag @flag-out)))]
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= 2 @out))
    (set-proc-fns-vec pipeline proc-fns-2)
    (reset! flag-out (prepare-flag))
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= -4 @out))
    (interrupt pipeline)))

(deftest simple-pipeline-decrease-length-by-1-test
  (let [flag-out (atom (prepare-flag))
        out (atom nil)
        proc-fns-1 [(fn [in _] (inc in))
                    (fn [_ out] (inc out))
                    (fn [_ out] (inc out))]
        proc-fns-2 [(fn [in _] (dec in))
                    (fn [_ out] (dec out))]
        pipeline (create-local-processing-pipeline
                   proc-fns-1
                   (fn [_ o] (reset! out o) (set-flag @flag-out)))]
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= 3 @out))
    (set-proc-fns-vec pipeline proc-fns-2)
    (reset! flag-out (prepare-flag))
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= -2 @out))
    (interrupt pipeline)))

(deftest simple-pipeline-decrease-length-by-n-test
  (let [flag-out (atom (prepare-flag))
        out (atom nil)
        proc-fns-1 [(fn [in _] (inc in))
                    (fn [_ out] (inc out))
                    (fn [_ out] (inc out))
                    (fn [_ out] (inc out))]
        proc-fns-2 [(fn [in _] (dec in))
                    (fn [_ out] (dec out))]
        pipeline (create-local-processing-pipeline
                   proc-fns-1
                   (fn [_ o] (reset! out o) (set-flag @flag-out)))]
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= 4 @out))
    (set-proc-fns-vec pipeline proc-fns-2)
    (reset! flag-out (prepare-flag))
    ((get-in-fn pipeline) 0)
    (await-flag @flag-out)
    (is (flag-set? @flag-out))
    (is (= -2 @out))
    (interrupt pipeline)))

(deftest get-pipeline-counts-test
  (let [flag-proc (prepare-flag)
        flag-out (prepare-flag)
        proc-fns [(fn [_ _] (set-flag flag-proc) 0)]
        pipeline (create-local-processing-pipeline
                   proc-fns
                   (fn [_ _] (set-flag flag-out)))]
    (is (= {0 {:out 0, :dropped 0}, :pipeline {:in 0, :dropped 0}} (get-counts pipeline)))
    ((get-in-fn pipeline) "foo")
    (await-flag flag-proc)
    (await-flag flag-out)
    (is (flag-set? flag-proc))
    (is (flag-set? flag-out))
    (is (= {0 {:out 1, :dropped 0}, :pipeline {:in 1, :dropped 0}} (get-counts pipeline)))
    (interrupt pipeline)))

