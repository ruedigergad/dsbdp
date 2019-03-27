;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Helper that are primarily used during experiments"}
  dsbdp.experiment-helper
  (:require
    [clojure.walk :refer :all]
    [clojure.pprint :refer :all]
    [dsbdp.byte-array-conversion :refer :all]
    [dsbdp.data-processing-dsl :refer :all]
    [dsbdp.processing-fn-utils :as utils])
  (:import
    (dsbdp ExperimentHelper)
    (java.util HashMap Map)))

(def pcap-byte-array-test-data
  "The byte array representation of a UDP packet for being used as dummy data."
  (byte-array
    (map byte [-5 -106 -57 84   15 -54 14 0          58 0 0 0   58 0 0 0         ; 16 byte pcap header
;              1422366459969231000 (Unix Timestamp)
               -1 -2 -3 -14 -15 -16    1 2 3 4 5 6          8 0                  ; 14 byte Ethernet header
;              FF:FE:FD:F2:F1:F0       01:02:03:04:05:06
               69 0 0 44   0 3 64 0   7 17 115 -57   1 2 3 4   -4 -3 -2 -1       ; 20 byte IP header
;                                                    1.2.3.4   252.253.254.255
               8 0     16 0    0 16 -25 -26                                      ; 8 byte UDP header
;              2048    4096
               97 98 99 100 101 102 103 104 105 106 107 108 109 110 111 112])))  ; 16 byte data "abcdefghijklmnop"

(def pcap-icmp-byte-array-test-data
  "The byte array representation of an ICMP packet for being used as dummy data."
  (byte-array
    (map byte [-5 -106 -57 84   15 -54 14 0          58 0 0 0   58 0 0 0                ; 16 byte pcap header
;              1422366459969231000 (Unix Timestamp)
               -1 -2 -3 -14 -15 -16    1 2 3 4 5 6          8 0                         ; 14 byte Ethernet header
;              FF:FE:FD:F2:F1:F0       01:02:03:04:05:06
               69 0 0 84   80 39 64 0   64 1 -127 -85   10 0 0 66   -43 -128 -119 20    ; IPv4 Header

               8 0 32 -66   101 25 0 3   6 16 -21 88   0 0 0 0                          ; ICMP Header
               ; ICMP Data
               -63 -23 0 0 0 0 0 0 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55
])))

(def pcap-tcp-byte-array-test-data
  "The byte array representation of a TCP packet for being used as dummy data."
  (byte-array
    (map byte [-5 -106 -57 84   15 -54 14 0          58 0 0 0   58 0 0 0         ; 16 byte pcap header
;              1422366459969231000 (Unix Timestamp)
               -1 -2 -3 -14 -15 -16    1 2 3 4 5 6          8 0                  ; 14 byte Ethernet header
;              FF:FE:FD:F2:F1:F0       01:02:03:04:05:06
               69 0 0 -7   -18 70 64 0   64 6 0 0   127 0 0 1   127 0 0 1        ; IPv4 Header

               -40 -30 -16 41   -12 -22 -42 -124   -38 79 -82 126   -128 24 4 0  ; TCP Header
               -2 -19 0 0   1 1 8 10   125 -32 27 -82   12 -28 -63 122

               ; TCP Payload
               77 69 83 83 65 71 69 10 99 111 110 116 101 110 116 45 108 101 110 103 116 104 58 49 57 10 101 120 112 105 114 101 115 58 48 10 100 101 115 116 105 110 97 116 105 111 110 58 47 116 111 112 105 99 47 116 101 115 116 116 111 112 105 99 46 102 111 111 10 115 117 98 115 99 114 105 112 116 105 111 110 58 49 10 112 114 105 111 114 105 116 121 58 52 10 109 101 115 115 97 103 101 45 105 100 58 73 68 92 99 99 111 108 105 110 45 51 55 57 57 55 45 49 52 55 51 49 52 55 50 53 50 51 54 51 45 49 54 92 99 49 92 99 49 92 99 49 92 99 49 10 116 105 109 101 115 116 97 109 112 58 49 52 55 51 49 52 55 50 54 51 55 49 50 10 10 123 34 97 34 58 49 50 51 44 34 98 34 58 34 120 121 122 34 125 0 10
])))



(defn create-no-op-proc-fns
  "Create a vector of n no-op functions."
  [n]
  (utils/create-proc-fn-vec-from-template
    '(fn [_ _] 0)
    '(fn [_ _] 1)
    n))

(defn create-inc-proc-fns
  "Create a vector of functions which each perform a single increment operation.
   The first function will increment the input argument and subsequent functions will increment the output argument."
  [n]
  (utils/create-proc-fn-vec-from-template
    '(fn [i _] (inc i))
    '(fn [_ o] (inc o))
    n))

(defn create-hashmap-inc-put-proc-fns
  "The general behaviour is analogous to create-inc-proc-fns, however, the results are stored in a map in which the index of the processing function is used as key to which the result of the operation is associated as value."
  [n]
  (let [o-sym 'o]
    (let [o-meta (vary-meta o-sym assoc :tag 'java.util.Map)]
     (utils/create-proc-fn-vec-from-template
       '(fn [i _] (doto (java.util.HashMap.) (.put (str :_idx_) (inc i))))
       '(fn [_ o-meta] (.put o-meta (str :_idx_) (inc (.get o-meta (str (dec :_idx_))))))
       n))))

(defn factorial
  "A purposely naive implementation for calculating factorials."
  ([n]
    (factorial 1N 1N n))
  ([result i n]
    (if (<= i n)
      (recur (* result i) (inc i) n)
      result)))

(defn create-factorial-proc-fns
  "Create a vector of processing functions that each calculate the factorial of the input data."
  [n]
  (utils/create-proc-fn-vec-from-template
    '(fn [i _] (dsbdp.experiment-helper/factorial i))
    '(fn [i _] (dsbdp.experiment-helper/factorial i))
     n))

(defn create-busy-sleep-proc-fns
  "Create a vector of processing functions that perform a busy sleep.
   The duration of the busy sleep is given as input data in nanoseconds."
  [n]
  (utils/create-proc-fn-vec-from-template
    '(fn [i _] (dsbdp.ExperimentHelper/busySleep ^long (i :_idx_)) 0)
    '(fn [i _] (dsbdp.ExperimentHelper/busySleep ^long (i :_idx_)) 0)
     n))

(def sample-pcap-processing-definition-rules
  [['timestamp '(timestamp-str-be 0) :string]
   ['capture-length '(int32be 8)]
   ['eth-src '(eth-mac-addr-str 22) :string]
   ['eth-dst '(eth-mac-addr-str 16) :string]
   ['ip-src '(ipv4-addr-str 42) :string]
   ['ip-dst '(ipv4-addr-str 46) :string]
   ['ip-ver '(int4h 30)]
   ['ip-length '(float (/ (int16 32) 65535))]
   ['ip-id '(float (/ (int16 34) 65535))]
   ['ip-ttl '(float (/ (int8 38) 255))]
   ['ip-protocol '(float (/ (int8 39) 255))]
   ['ip-checksum '(float (/ (int16 40) 65535))]
   ['udp-src '(float (/ (int16 50) 65535))]
   ['udp-dst '(float (/ (int16 52) 65535))]
   ['udp-length '(float (/ (int16 54) 65535))]
   ['udp-checksum '(float (/ (int16 56) 65535))]
   ['udp-payload '(ba-to-str 58 16) :string]])

(def sample-pcap-processing-definition-csv
  {:output-type :csv-str
   :rules sample-pcap-processing-definition-rules})

(def sample-pcap-processing-definition-json
  {:output-type :json-str
   :rules sample-pcap-processing-definition-rules})

(def sample-pcap-processing-definition-clj-map
  {:output-type :clj-map
   :rules sample-pcap-processing-definition-rules})

(def sample-pcap-processing-definition-java-map
  {:output-type :java-map
   :rules sample-pcap-processing-definition-rules})

(def synthetic-low-throughput-self-adaptivity-processing-fns
  [(fn [i _] (dsbdp.ExperimentHelper/busySleep 200000) i)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 100000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 100000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  50000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  50000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  50000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  50000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  25000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  25000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  25000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  20500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  25000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  25000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  25000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  25000) o)])

(def synthetic-average-throughput-self-adaptivity-processing-fns
  [(fn [i _] (dsbdp.ExperimentHelper/busySleep 20000) i)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 10000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 10000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  5000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  5000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  5000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  5000) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  2500) o)])

(def synthetic-high-throughput-self-adaptivity-processing-fns
  [(fn [i _] (dsbdp.ExperimentHelper/busySleep 5000) i)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 2500) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 1250) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 1250) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 1250) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep 1250) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)
   (fn [_ o] (dsbdp.ExperimentHelper/busySleep  625) o)])

