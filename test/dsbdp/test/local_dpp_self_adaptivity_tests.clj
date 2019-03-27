;;;
;;;   Copyright 2016 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for local self-adaptive data processing pipelines."}
  dsbdp.test.local-dpp-self-adaptivity-tests
  (:require
    [clj-assorted-utils.util :refer :all]
    [clojure.test :refer :all]
    [dsbdp.experiment-helper :refer :all]
    [dsbdp.local-dpp-self-adaptivity :refer :all]
    [dsbdp.processing-fn-utils :refer :all]))

(def four-staged-pipeline-stats-example-1
  {3 {:out 17460, :dropped 1082},
   2 {:out 18542, :dropped 1697469},
   1 {:out 1716523, :dropped 611647},
   0 {:out 2328170, :dropped 2911553},
   :pipeline {:in 5239723, :dropped 7764598}})

(def four-staged-pipeline-stats-example-2
  {3 {:out 18460, :dropped 2082},
   2 {:out 19542, :dropped 1698469},
   1 {:out 1717523, :dropped 612647},
   0 {:out 2329170, :dropped 2912553},
   :pipeline {:in 5240723, :dropped 7765598}})

(deftest stat-delta-test-1
  (let [stat-1 {0 {:out 1, :dropped 2}, 1 {:out 3, :dropped 4}, :pipeline {:in 5, :dropped 6}}
        stat-2 {0 {:out 10, :dropped 20}, 1 {:out 30, :dropped 40}, :pipeline {:in 50, :dropped 60}}
        expected-delta-1 {0 {:out-delta 1, :dropped-delta 2}, 1 {:out-delta 3, :dropped-delta 4},
                          :pipeline {:in-delta 5, :dropped-delta 6}}
        expected-delta-2 {0 {:out-delta 9, :dropped-delta 18}, 1 {:out-delta 27, :dropped-delta 36},
                          :pipeline {:in-delta 45, :dropped-delta 54}}
        stat-d-cntr (create-stat-delta-counter 2)]
    (is (= expected-delta-1 (stat-d-cntr stat-1)))
    (is (= expected-delta-2 (stat-d-cntr stat-2)))))

(deftest stat-delta-test-2
  (let [expected-delta-1 {0 {:out-delta 2328170, :dropped-delta 2911553}, 1 {:out-delta 1716523, :dropped-delta 611647},
                          2 {:out-delta 18542, :dropped-delta 1697469}, 3 {:out-delta 17460, :dropped-delta 1082},
                          :pipeline {:in-delta 5239723, :dropped-delta 7764598}}
        expected-delta-2 {0 {:out-delta 1000, :dropped-delta 1000}, 1 {:out-delta 1000, :dropped-delta 1000},
                          2 {:out-delta 1000, :dropped-delta 1000}, 3 {:out-delta 1000, :dropped-delta 1000},
                          :pipeline {:in-delta 1000, :dropped-delta 1000}}
        stat-d-cntr (create-stat-delta-counter 4)]
    (is (= expected-delta-1 (stat-d-cntr four-staged-pipeline-stats-example-1)))
    (is (= expected-delta-2 (stat-d-cntr four-staged-pipeline-stats-example-2)))))

(deftest simple-repetition-detection-test-1
  (let [detector (create-repetition-detector 3)]
    (is (not (detector (fn [] true))))
    (is (not (detector (fn [] true))))
    (is (detector (fn [] true)))
    (is (not (detector (fn [] true))))))

(deftest simple-repetition-detection-test-2
  (let [detector (create-repetition-detector 3)]
    (is (not (detector (fn [] true))))
    (is (not (detector (fn [] true))))
    (is (not (detector (fn [] false))))
    (is (not (detector (fn [] true))))
    (is (not (detector (fn [] true))))
    (is (detector (fn [] true)))
    (is (not (detector (fn [] true))))))

(deftest moving-average-calculator-test-1
  (let [mvg-avg-calc (create-moving-average-calculator 3)]
    (is (= 0 (mvg-avg-calc)))
    (mvg-avg-calc 1000)
    (is (= (/ 1000 3) (mvg-avg-calc)))
    (mvg-avg-calc 1000)
    (is (= (/ 2000 3) (mvg-avg-calc)))
    (mvg-avg-calc 1000)
    (is (= 1000 (mvg-avg-calc)))))

(deftest moving-average-calculator-test-2
  (let [mvg-avg-calc (create-moving-average-calculator 3)]
    (mvg-avg-calc 1000)
    (mvg-avg-calc 2000)
    (mvg-avg-calc 3000)
    (is (= 2000 (mvg-avg-calc)))))

(deftest moving-average-calculator-test-3
  (let [mvg-avg-calc (create-moving-average-calculator 3)]
    (mvg-avg-calc 1000)
    (mvg-avg-calc 1000)
    (mvg-avg-calc 1000)
    (mvg-avg-calc 2000)
    (mvg-avg-calc 3000)
    (is (= 2000 (mvg-avg-calc)))))

(deftest detect-repeated-drop-test-1
  (let [deltas {0 {:out-delta 10, :dropped-delta 10}, 1 {:out-delta 10, :dropped-delta 10},
                :pipeline {:in-delta 10, :dropped-delta 10}}
        detector (create-drop-detector 3 (count deltas) 5)]
    (is (= [false false false] (detector deltas)))
    (is (= [false false false] (detector deltas)))
    (is (= [true true true] (detector deltas)))))

(deftest detect-repeated-drop-test-2
  (let [deltas {0 {:out-delta 10, :dropped-delta 10}, 1 {:out-delta 10, :dropped-delta 10},
                2 {:out-delta 10, :dropped-delta 10}, 3 {:out-delta 10, :dropped-delta 10},
                :pipeline {:in-delta 10, :dropped-delta 10}}
        detector (create-drop-detector 4 (count deltas) 5)]
    (is (= [false false false false false] (detector deltas)))
    (is (= [false false false false false] (detector deltas)))
    (is (= [false false false false false] (detector deltas)))
    (is (= [true true true true true] (detector deltas)))))

(deftest get-drop-indices-test-1
  (let [drop-detector-vec [false false false false]
        expected []]
    (is (=  expected (get-drop-indices drop-detector-vec)))))

(deftest get-drop-indices-test-2
  (let [drop-detector-vec [false true false false]
        expected [1]]
    (is (=  expected (get-drop-indices drop-detector-vec)))))

(deftest get-drop-indices-test-3
  (let [drop-detector-vec [false true false true]
        expected [1 3]]
    (is (=  expected (get-drop-indices drop-detector-vec)))))

(deftest get-non-drop-indices-test-1
  (let [drop-detector-vec [false false false false]
        expected [0 1 2 3]]
    (is (=  expected (get-non-drop-indices drop-detector-vec)))))

(deftest get-non-drop-indices-test-2
  (let [drop-detector-vec [false true false false]
        expected [0 2 3]]
    (is (=  expected (get-non-drop-indices drop-detector-vec)))))

(deftest get-non-drop-indices-test-3
  (let [drop-detector-vec [false true false true]
        expected [0 2]]
    (is (=  expected (get-non-drop-indices drop-detector-vec)))))

(deftest update-fns-mapping-for-three-staged-pipeline-test-1
  (let [mapping-updater (create-mapping-updater)
        original-mapping [14 1 1]
        drop-detector-vec [false false false false]
        expected-mapping [14 1 1]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-four-staged-pipeline-test-1
  (let [mapping-updater (create-mapping-updater)
        original-mapping [14 1 1 1]
        drop-detector-vec [false false false false false]
        expected-mapping [14 1 1 1]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-three-staged-pipeline-test-2
  (let [mapping-updater (create-mapping-updater)
        original-mapping [14 1 1]
        drop-detector-vec [true false false false]
        expected-mapping [12 2 2]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-four-staged-pipeline-test-2
  (let [mapping-updater (create-mapping-updater)
        original-mapping [14 1 1 1]
        drop-detector-vec [true false false false false]
        expected-mapping [11 2 2 2]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-three-staged-pipeline-test-3
  (let [mapping-updater (create-mapping-updater)
        original-mapping [10 5 1]
        drop-detector-vec [true true false false]
        expected-mapping [10 4 2]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-four-staged-pipeline-test-3
  (let [mapping-updater (create-mapping-updater)
        original-mapping [8 5 5 1]
        drop-detector-vec [true true true false false]
        expected-mapping [8 5 4 2]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-three-staged-pipeline-test-4
  (let [mapping-updater (create-mapping-updater)
        original-mapping [5 5 5]
        drop-detector-vec [false false true false]
        expected-mapping [5 6 4]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-four-staged-pipeline-test-4
  (let [mapping-updater (create-mapping-updater)
        original-mapping [5 5 5 5]
        drop-detector-vec [false false false true false]
        expected-mapping [5 5 6 4]]
    (is (= expected-mapping (mapping-updater original-mapping drop-detector-vec)))))

(deftest update-fns-mapping-for-three-staged-pipeline-test-5
  (let [mapping-updater (create-mapping-updater)
        original-mapping [5 5 5]
        drop-detector-vec-1 [false false true false]
        drop-detector-vec-2 [false true false false]
        drop-detector-vec-3 [true false false false]
        expected-mapping-1 [5 6 4]
        expected-mapping-2 [6 5 4]
        expected-mapping-3 [6 5 4]]
    (is (= expected-mapping-1 (mapping-updater original-mapping drop-detector-vec-1)))
    (is (= expected-mapping-2 (mapping-updater expected-mapping-1 drop-detector-vec-2)))
    (is (= expected-mapping-3 (mapping-updater expected-mapping-2 drop-detector-vec-3)))))

(deftest update-fns-mapping-for-four-staged-pipeline-test-5
  (let [mapping-updater (create-mapping-updater)
        original-mapping [5 5 5 5]
        drop-detector-vec-1 [false false false true false]
        drop-detector-vec-2 [false false true false false]
        drop-detector-vec-3 [false true false false false]
        expected-mapping-1 [5 5 6 4]
        expected-mapping-2 [5 6 5 4]
        expected-mapping-3 [6 5 5 4]]
    (is (= expected-mapping-1 (mapping-updater original-mapping drop-detector-vec-1)))
    (is (= expected-mapping-2 (mapping-updater expected-mapping-1 drop-detector-vec-2)))
    (is (= expected-mapping-3 (mapping-updater expected-mapping-2 drop-detector-vec-3)))))

(deftest update-fns-mapping-for-four-staged-pipeline-test-6
  (let [mapping-updater (create-mapping-updater)
        original-mapping [3 4 4 4]
        drop-detector-vec-1 [true false false false false]
        drop-detector-vec-2 [true true false false false]
        drop-detector-vec-3 [true true false false false]
        expected-mapping-1 [1 4 5 5]
        expected-mapping-2 [1 2 6 6]
        expected-mapping-3 [1 1 6 7]]
    (is (= expected-mapping-1 (mapping-updater original-mapping drop-detector-vec-1)))
    (is (= expected-mapping-2 (mapping-updater expected-mapping-1 drop-detector-vec-2)))
    (is (= expected-mapping-3 (mapping-updater expected-mapping-2 drop-detector-vec-3)))))

(deftest self-adaptivity-controller-test-1
  (let [orig-proc-fns (create-no-op-proc-fns 15)
        stat-1 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 1},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        stat-2 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 20},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        stat-3 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 40},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        expected-mapping-1 [5 6 4]
        expected-mapping-2 [5 6 4]
        expected-mapping-3 [6 5 4]
        controller-cfg {:inactivity 1 :threshold 10 :repetition 1}
        mapping (atom [5 5 5])
        proc-fns (atom (combine-proc-fns-vec @mapping orig-proc-fns))
        mock-pipeline (fn [_] (fn [& _]))
        self-adaptivity-controller (create-self-adaptivity-controller
                                     controller-cfg
                                     mock-pipeline
                                     orig-proc-fns
                                     mapping)]
    (update-stats self-adaptivity-controller stat-1)
    (is (= expected-mapping-1 @mapping))
    (update-stats self-adaptivity-controller stat-2)
    (is (= expected-mapping-2 @mapping))
    (update-stats self-adaptivity-controller stat-3)
    (is (= expected-mapping-3 @mapping))))

(deftest self-adaptivity-controller-test-2
  (let [orig-proc-fns (create-no-op-proc-fns 15)
        stat-1 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 1},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        stat-2 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 1},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        stat-3 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 20},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        expected-mapping-1 [5 6 4]
        expected-mapping-2 [5 6 4]
        expected-mapping-3 [6 5 4]
        controller-cfg {:inactivity 1 :threshold 10 :repetition 1}
        mapping (atom [5 5 5])
        proc-fns (atom (combine-proc-fns-vec @mapping orig-proc-fns))
        mock-pipeline (fn [_] (fn [& _]))
        self-adaptivity-controller (create-self-adaptivity-controller
                                     controller-cfg
                                     mock-pipeline
                                     orig-proc-fns
                                     mapping)]
    (update-stats self-adaptivity-controller stat-1)
    (is (= expected-mapping-1 @mapping))
    (update-stats self-adaptivity-controller stat-2)
    (is (= expected-mapping-2 @mapping))
    (update-stats self-adaptivity-controller stat-3)
    (is (= expected-mapping-3 @mapping))))

(deftest self-adaptivity-controller-test-3
  (let [orig-proc-fns (create-no-op-proc-fns 15)
        stat-1 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 1},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        stat-2 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 20},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        stat-3 {:pipeline {:in 1, :dropped 1},
                0 {:out 1, :dropped 20},
                1 {:out 1, :dropped 20},
                2 {:out 1, :dropped 1}}
        expected-mapping-1 [5 6 4]
        expected-mapping-2 [5 6 4]
        expected-mapping-3 [5 6 4]
        controller-cfg {:inactivity 1 :threshold 10 :repetition 1}
        mapping (atom [5 5 5])
        proc-fns (atom (combine-proc-fns-vec @mapping orig-proc-fns))
        mock-pipeline (fn [_] (fn [& _]))
        self-adaptivity-controller (create-self-adaptivity-controller
                                     controller-cfg
                                     mock-pipeline
                                     orig-proc-fns
                                     mapping)]
    (update-stats self-adaptivity-controller stat-1)
    (is (= expected-mapping-1 @mapping))
    (update-stats self-adaptivity-controller stat-2)
    (is (= expected-mapping-2 @mapping))
    (update-stats self-adaptivity-controller stat-3)
    (is (= expected-mapping-3 @mapping))))

