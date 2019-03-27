;;;
;;;   Copyright 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for parallelized data processing"}
  dsbdp.test.parallel-processing-tests
  (:require
    [clojure.test :refer :all]
    [clj-assorted-utils.util :refer :all]
    [dsbdp.parallel-processing :refer :all]))

(deftest simple-pmap-processor-test
  (let [proc-fn (fn [x] (* 2 x))
        batch-size 3
        out-data (atom [])
        flag (prepare-flag batch-size)
        out-fn (fn [data]
                 (swap! out-data conj data)
                 (set-flag flag))
        pmap-proc (create-simple-pmap-processor proc-fn batch-size out-fn)]
    (is (= [] @out-data))
    (pmap-proc 1)
    (is (= [] @out-data))
    (pmap-proc 2)
    (is (= [] @out-data))
    (pmap-proc 3)
    (await-flag flag)
    (is (= [2 4 6] @out-data))))

(deftest simple-reducers-map-processor-test
  (let [proc-fn (fn [x] (* 2 x))
        batch-size 4
        partition-size 2
        out-data (atom [])
        flag (prepare-flag batch-size)
        out-fn (fn [data]
                 (swap! out-data conj data)
                 (set-flag flag))
        pmap-proc (create-simple-reducers-map-processor proc-fn batch-size partition-size out-fn)]
    (is (= [] @out-data))
    (pmap-proc 1)
    (is (= [] @out-data))
    (pmap-proc 2)
    (is (= [] @out-data))
    (pmap-proc 3)
    (is (= [] @out-data))
    (pmap-proc 4)
    (await-flag flag)
    (is (= [2 4 6 8] @out-data))))

