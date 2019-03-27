;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for a helpers that are primarily used for experiments"}
  dsbdp.test.experiment-helper-tests
  (:require
    [clojure.test :refer :all]
    [dsbdp.data-processing-dsl :refer :all] 
    [dsbdp.experiment-helper :refer :all])
  (:import
    (dsbdp ExperimentHelper)
    (java.util HashMap Map)))

(deftest create-no-op-proc-fns-test
  (let [proc-fns (create-no-op-proc-fns 4)]
    (is (= 4 (count proc-fns)))
    (is (= 0 ((first proc-fns) nil nil)))
    (is (= 1 ((nth proc-fns 1) nil nil)))
    (is (= 1 ((last proc-fns) nil nil)))))

(deftest create-inc-proc-fns-test
  (let [proc-fns (create-inc-proc-fns 4)]
    (is (= 4 (count proc-fns)))
    (is (= 2 ((first proc-fns) 1 nil)))
    (is (= 2 ((nth proc-fns 1) nil 1)))
    (is (= 2 ((last proc-fns) nil 1)))))

(deftest create-hashmap-inc-put-proc-fns-test
  (let [proc-fns (create-hashmap-inc-put-proc-fns 4)]
    (is (= 4 (count proc-fns)))
    (let [^Map m ((first proc-fns) 0 nil)]
      (is (= HashMap (type m)))
      (is (= 1 (.get m "0")))
      ((nth proc-fns 1) nil m)
      ((nth proc-fns 2) nil m)
      ((nth proc-fns 3) nil m)
      (is (= 2 (.get m "1")))
      (is (= 3 (.get m "2")))
      (is (= 4 (.get m "3"))))))

(deftest factorial-test
  (is (= 1 (factorial 1)))
  (is (= 2 (factorial 2)))
  (is (= 6 (factorial 3)))
  (is (= 24 (factorial 4))))

(deftest create-factorial-proc-fns-test
  (let [proc-fns (create-factorial-proc-fns 4)]
    (is (= 4 (count proc-fns)))
    (is (= 6 ((first proc-fns) 3 nil)))
    (is (= 6 ((nth proc-fns 1) 3 nil)))
    (is (= 6 ((last proc-fns) 3 nil)))))

;(deftest sample-pcap-processing-definition-json-test
;  (let [processing-fn (create-proc-fn sample-pcap-processing-definition-json)]
;    (is (=
;         (str
;           "{\"timestamp\":\"2015-01-27_14:47:39\",\"capture-length\":58,"
;           "\"eth-src\":\"01:02:03:04:05:06\",\"eth-dst\":\"FF:FE:FD:F2:F1:F0\","
;           "\"ip-src\":\"1.2.3.4\",\"ip-dst\":\"252.253.254.255\",\"ip-ver\":4,\"ip-length\":6.7139696E-4,"
;           "\"ip-id\":4.5777066E-5,\"ip-ttl\":0.02745098,\"ip-protocol\":0.06666667,\"ip-checksum\":0.45226216,"
;           "\"udp-src\":0.031250477,\"udp-dst\":0.06250095,\"udp-length\":2.4414435E-4,"
;           "\"udp-checksum\":0.9058671,\"udp-payload\":\"abcdefghijklmnop\"}")
;         (str (processing-fn pcap-byte-array-test-data))))))

;(deftest sample-pcap-processing-definition-clj-map-test
;  (let [processing-fn (create-proc-fn sample-pcap-processing-definition-clj-map)]
;    (is (=
;         {"timestamp" "2015-01-27_14:47:39","capture-length" 58,
;          "eth-src" "01:02:03:04:05:06","eth-dst" "FF:FE:FD:F2:F1:F0",
;          "ip-src" "1.2.3.4","ip-dst" "252.253.254.255","ip-ver" 4,"ip-length" 6.7139696E-4,
;          "ip-id" 4.5777066E-5,"ip-ttl" 0.02745098,"ip-protocol" 0.06666667,"ip-checksum" 0.45226216,
;          "udp-src" 0.031250477,"udp-dst" 0.06250095,"udp-length" 2.4414435E-4,
;          "udp-checksum" 0.9058671,"udp-payload" "abcdefghijklmnop"}
;         (processing-fn pcap-byte-array-test-data)))
;    (println (processing-fn pcap-byte-array-test-data))
;    ))

(deftest busy-sleep-test-1
  (let [start-time (System/nanoTime)
        _ (ExperimentHelper/busySleep 100000)
        end-time (System/nanoTime)
        time-delta (- end-time start-time)]
    (is (> time-delta 100000))
    (is (< time-delta 200000))
    ))

(deftest busy-sleep-test-2
  (let [start-time (System/nanoTime)
        _ (ExperimentHelper/busySleep 1000000)
        end-time (System/nanoTime)
        time-delta (- end-time start-time)]
    (is (> time-delta 1000000))
    (is (< time-delta 2500000))
    ))

(deftest busy-sleep-proc-fns-test-1
  (let [proc-fns (create-busy-sleep-proc-fns 4)
        start-time (atom nil)
        end-time (atom nil)
        in-data [100000 200000 300000 400000]]
    (is (= 4 (count proc-fns)))
    (reset! start-time (System/nanoTime))
    ((proc-fns 0) in-data nil)
    (reset! end-time (System/nanoTime))
    (is (> (- @end-time @start-time) 100000))
    (is (< (- @end-time @start-time) 300000))
    (reset! start-time (System/nanoTime))
    ((proc-fns 1) in-data nil)
    (reset! end-time (System/nanoTime))
    (is (> (- @end-time @start-time) 200000))
    (is (< (- @end-time @start-time) 400000))
    (reset! start-time (System/nanoTime))
    ((proc-fns 2) in-data nil)
    (reset! end-time (System/nanoTime))
    (is (> (- @end-time @start-time) 300000))
    (is (< (- @end-time @start-time) 500000))
    (reset! start-time (System/nanoTime))
    ((proc-fns 3) in-data nil)
    (reset! end-time (System/nanoTime))
    (is (> (- @end-time @start-time) 400000))
    (is (< (- @end-time @start-time) 600000))))

