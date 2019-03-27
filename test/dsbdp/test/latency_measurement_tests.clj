;;;
;;;   Copyright 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for measuring latency."}
  dsbdp.test.latency-measurement-tests
  (:require [clojure.test :refer :all])
  (:import (dsbdp LatencyProbe LatencyProbeCollector)))

(deftest simple-latency-probe-test
  (let [latency-probe (LatencyProbe.)]
    (.done latency-probe)
    (is (< 0 (.getDelta latency-probe)))))

(deftest simpla-latency-probe-collector-test
  (let [lp1 (LatencyProbe.)
        lp2 (LatencyProbe.)
        lp3 (LatencyProbe.)
        latency-probe-collector (LatencyProbeCollector.)]
    (.done lp1)
    (.done lp2)
    (.done lp3)
    (is (= 0.0 (.getMean latency-probe-collector)))
    (.addProbe latency-probe-collector lp1)
    (is (= (double (.getDelta lp1)) (.getMean latency-probe-collector)))
    (.addProbe latency-probe-collector lp2)
    (is (=
         (* 0.5 (+
                 (double (.getDelta lp1))
                 (double (.getDelta lp2))))
         (.getMean latency-probe-collector)))
    (.reset latency-probe-collector)
    (is (= 0.0 (.getMean latency-probe-collector)))
    (.addProbe latency-probe-collector lp3)
    (is (= (double (.getDelta lp3)) (.getMean latency-probe-collector)))))

