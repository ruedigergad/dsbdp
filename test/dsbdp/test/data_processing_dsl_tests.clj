;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for data processing DSL"}
  dsbdp.test.data-processing-dsl-tests
  (:require [clojure.test :refer :all]
            [dsbdp.data-processing-dsl :refer :all]
            [dsbdp.experiment-helper :refer :all]) 
  (:import
    (java.util ArrayList HashMap List Map)
    (java.nio.file Files Paths)))



(deftest simple-documenation-example-test-1
  (let [in-vector [1.23 "FOO" 42 "bar" "baz"]
        expected-clj-map {"myFloat" 1.23, "myStr" "foo", "myRatio" 0.42, "myStr2" "barbaz"}
        expected-json-str "{\"myFloat\":1.23,\"myStr\":\"foo\",\"myRatio\":0.42,\"myStr2\":\"barbaz\"}"
        expected-csv-str "1.23,\"foo\",0.42,\"barbaz\""
        dsl-expression {:output-type :csv-str
                        :rules [['myFloat '(nth 0)]
                                ['myStr '(clojure.string/lower-case (nth 1)) :string]
                                ['myRatio '(/ (nth 2) 100.0)]
                                ['myStr2 '(str (nth 3) (nth 4)) :string]]}
        data-proc-fn-csv-str (create-proc-fn dsl-expression)
        data-proc-fn-json-str (create-proc-fn (assoc dsl-expression :output-type :json-str))
        data-proc-fn-clj-map (create-proc-fn (assoc dsl-expression :output-type :clj))
        data-proc-fn-java-map (create-proc-fn (assoc dsl-expression :output-type :java))
        ]
    (is (= expected-csv-str (str (data-proc-fn-csv-str in-vector))))
    (is (= expected-json-str (str (data-proc-fn-json-str in-vector))))
    (is (= expected-clj-map (data-proc-fn-java-map in-vector)))
    (is (= expected-clj-map (data-proc-fn-clj-map in-vector)))))



;;;
;;; Tests for byte array input and various output types.
;;;
(deftest byte-array-to-java-map-test
  (let [expected {"udpSrc" 2048, "udpDst" 4096}
        dsl-expression {:output-type :java
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest byte-array-to-clj-map-test
  (let [expected {"udpSrc" 2048, "udpDst" 4096}
        dsl-expression {:output-type :clj
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (map? result))
    (is (= expected result))))

(deftest byte-array-to-csv-str-test
  (let [expected "2048,4096"
        dsl-expression {:output-type :csv-str
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (= expected (str result)))))

(deftest byte-array-to-json-str-test
  (let [expected "{\"udpSrc\":2048,\"udpDst\":4096}"
        dsl-expression {:output-type :json-str
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (= expected (str result)))))

(deftest byte-array-to-csv-str-qm-test
  (let [expected "\"2048\",4096"
        dsl-expression {:output-type :csv-str
                        :rules [['udpSrc '(int16 50) :string]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (= expected (str  result)))))

(deftest byte-array-to-json-str-qm-test
  (let [expected "{\"udpSrc\":2048,\"udpDst\":\"4096\"}"
        dsl-expression {:output-type :json-str
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52) :string]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (= expected (str result)))))

(deftest output-type-defaults-to-java-map-test
  (let [expected {"udpSrc" 2048, "udpDst" 4096}
        dsl-expression {:output-type :this-is-no-supported-output-type
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (instance? Map result))
    (is (= expected result))))



;;;
;;; Tests for referencing already extracted fields.
;;;
(deftest simple-combination-of-extracted-fields-test
  (let [expected {"udpSrc" 2048, "udpDst" 4096, "combined" "2048 -> 4096"}
        dsl-expression {:output-type :java
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]
                                ['combined '(str udpSrc " -> " udpDst)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (= expected result))))



;;;
;;; Tests for incremental data processing.
;;;
(deftest incremental-byte-array-to-java-map-test
  (let [expected {"udpSrc" 2048, "udpDst" 4096, "foo" :bar}
        dsl-expression {:output-type :java#inc
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        input  (HashMap. {"foo" :bar})
        result (data-processing-fn pcap-byte-array-test-data input)]
    (is (instance? Map result))
    (is (identical? input result))
    (is (= expected result))))

(deftest incremental-byte-array-to-clj-map-test
  (let [expected {"udpSrc" 2048, "udpDst" 4096, "foo" :bar}
        dsl-expression {:output-type :clj#inc
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        input  {"foo" :bar}
        result (data-processing-fn pcap-byte-array-test-data input)]
    (is (map? result))
    (is (not (identical? input result)))
    (is (= expected result))))

(deftest incremental-byte-array-to-csv-str-test
  (let [expected "foo,bar,2048,4096"
        dsl-expression {:output-type :csv-str#inc
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        input (doto (java.lang.StringBuilder.) (.append "foo,bar,"))
        result (data-processing-fn pcap-byte-array-test-data input)]
    (is (= expected (str result)))))

(deftest incremental-byte-array-to-json-str-test
  (let [expected "{\"foo\":\"bar\",\"udpSrc\":2048,\"udpDst\":4096}"
        dsl-expression {:output-type :json-str#inc
                        :rules [['udpSrc '(int16 50)]
                                ['udpDst '(int16 52)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        input (doto (java.lang.StringBuilder.) (.append "{\"foo\":\"bar\"}"))
        result (data-processing-fn pcap-byte-array-test-data input)]
    (is (= expected (str result)))))



;;;
;;; Tests for more complex data processing definitions/functions.
;;;
(deftest byte-array-to-java-map-with-additional-operation-test
  (let [expected {"udpSrc" 1024, "udpDst" 2048}
        dsl-expression {:output-type :java
                        :rules [['udpSrc '(/ (int16 50) 2)]
                                ['udpDst '(/ (int16 52) 2)]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest byte-array-to-java-map-with-additional-operation-and-float-type-test
  (let [expected {"udpSrc" (float 0.031250477), "udpDst" (float 0.06250095)}
        dsl-expression {:output-type :java
                        :rules [['udpSrc '(float (/ (int16 50) 65535))]
                                ['udpDst '(float (/ (int16 52) 65535))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest byte-array-to-java-map-with-additional-operation-and-two-data-values-test
  (let [expected {"quotient" 2}
        dsl-expression {:output-type :java
                        :rules [['quotient '(/ (int16 52) (int16 50))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (instance? Map result))
    (is (= expected result))))



;;;
;;; Tests for other input data types.
;;;
(deftest clojure-vector-to-java-map-with-additional-operation-and-two-data-values-test
  (let [expected {"quotient" 2}
        input-data [1 8 1 1 1 4 1 1]
        dsl-expression {:output-type :java
                        :rules [['quotient '(/ (nth 1) (nth 5))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn input-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest clojure-list-to-java-map-with-additional-operation-and-two-data-values-test
  (let [expected {"quotient" 2}
        input-data '(1 8 1 1 1 4 1 1)
        dsl-expression {:output-type :java
                        :rules [['quotient '(/ (nth 1) (nth 5))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn input-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest java-list-to-java-map-with-additional-operation-and-two-data-values-test
  (let [expected {"quotient" 2}
        input-data (reduce (fn [l v] (.add ^List l v) l) (ArrayList.) [1 8 1 1 1 4 1 1])
        dsl-expression {:output-type :java
                        :rules [['quotient '(/ (nth 1) (nth 5))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn input-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest clojure-map-to-java-map-with-additional-operation-and-two-data-values-test
  (let [expected {"quotient" 2}
        input-data {"x" 12, "y" 42, :z 6}
        dsl-expression {:output-type :java
                        :rules [['quotient '(/ (get "x") (get :z))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn input-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest java-map-to-java-map-with-additional-operation-and-two-data-values-test
  (let [expected {"quotient" 2}
        input-data (doto (HashMap.) (.put "x" 12) (.put "y" 42) (.put :z 6))
        dsl-expression {:output-type :java
                        :rules [['quotient '(/ (get "x") (get :z))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn input-data)]
    (is (instance? Map result))
    (is (= expected result))))



;;;
;;; Tests for returning identity.
;;;
(deftest input-to-str-test
  (let [expected {"data-str" "[1 8 1 1 1 4 1 1]"}
        input-data [1 8 1 1 1 4 1 1]
        dsl-expression {:output-type :clj
                        :rules [['data-str '(str (identity))]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn input-data)]
    (is (instance? Map result))
    (is (= expected result))))



;;;
;;; Tests for generating vectors of functions that can be used in the data processing pipeline.
;;;
(deftest combine-proc-fns-1st-test
  (let [input-ba (byte-array (map byte [0 1 2 3 4 5 6 7 8 9]))
        expected {"a" 0, "b" 1, "c" 2, "d" 3}
        dsl-expression {:output-type :clj
                        :rules [['a '(int8 0)]
                                ['b '(int8 1)]
                                ['c '(int8 2)]
                                ['d '(int8 3)]
                                ['e '(int8 4)]
                                ['f '(int8 5)]
                                ['g '(int8 6)]
                                ['h '(int8 7)]
                                ['i '(int8 8)]
                                ['j '(int8 9)]]}
        proc-fn (combine-proc-fns dsl-expression 0 4)]
    (is (= expected (proc-fn input-ba)))))

(deftest combine-proc-fns-nth-test
  (let [input-ba (byte-array (map byte [0 1 2 3 4 5 6 7 8 9]))
        expected {"e" 4, "f" 5, "g" 6}
        dsl-expression {:output-type :clj
                        :rules [['a '(int8 0)]
                                ['b '(int8 1)]
                                ['c '(int8 2)]
                                ['d '(int8 3)]
                                ['e '(int8 4)]
                                ['f '(int8 5)]
                                ['g '(int8 6)]
                                ['h '(int8 7)]
                                ['i '(int8 8)]
                                ['j '(int8 9)]]}
        proc-fn (combine-proc-fns dsl-expression 4 7)]
    (is (= expected (proc-fn input-ba {})))))

(deftest dsl-expression-to-combined-function-vector-test-1
  (let [input-ba (byte-array (map byte [0 1 2 3 4 5 6 7 8 9]))
        expected-0 {"a" 0, "b" 1, "c" 2, "d" 3}
        expected-1 {"e" 4, "f" 5, "g" 6}
        expected-2 {"h" 7, "i" 8}
        expected-3 {"j" 9}
        dsl-expression {:output-type :clj
                        :rules [['a '(int8 0)]
                                ['b '(int8 1)]
                                ['c '(int8 2)]
                                ['d '(int8 3)]
                                ['e '(int8 4)]
                                ['f '(int8 5)]
                                ['g '(int8 6)]
                                ['h '(int8 7)]
                                ['i '(int8 8)]
                                ['j '(int8 9)]]}
        proc-fns-vec (combine-proc-fns-vec
                       [4 3 2 1]
                       dsl-expression)]
    (is (vector? proc-fns-vec))
    (is (= expected-0 ((proc-fns-vec 0) input-ba {})))
    (is (= expected-1 ((proc-fns-vec 1) input-ba {})))
    (is (= expected-2 ((proc-fns-vec 2) input-ba {})))
    (is (= expected-3 ((proc-fns-vec 3) input-ba {})))
    (is (= (merge
             expected-0 expected-1 expected-2 expected-3)
           (->> {}
             ((proc-fns-vec 0) input-ba)
             ((proc-fns-vec 1) input-ba)
             ((proc-fns-vec 2) input-ba)
             ((proc-fns-vec 3) input-ba))))))



;;;
;;; Tests for nested output data structures.
;;;
(deftest byte-array-packet-to-nested-java-map-test
  (let [expected {"len" 58,
                  "data" {"dst" "FF:FE:FD:F2:F1:F0",
                          "src" "01:02:03:04:05:06",
                          "data" {"src" "1.2.3.4",
                                  "dst" "252.253.254.255",
                                  "data" {"src" 2048
                                          "dst" 4096}}}}
        dsl-expression {:output-type :java
                        :rules [['len '(int32be 8)]
                                ['data [['dst '(eth-mac-addr-str 16)]
                                        ['src '(eth-mac-addr-str 22)]
                                        ['data [['src '(ipv4-addr-str 42)]
                                                ['dst '(ipv4-addr-str 46)]
                                                ['data [['src '(int16 50)]
                                                        ['dst '(int16 52)]]]]]]]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (instance? Map result))
    (is (= expected result))))

(deftest byte-array-packet-to-nested-clj-map-test
  (let [expected {"len" 58,
                  "data" {"dst" "FF:FE:FD:F2:F1:F0",
                          "src" "01:02:03:04:05:06",
                          "data" {"src" "1.2.3.4",
                                  "dst" "252.253.254.255",
                                  "data" {"src" 2048
                                          "dst" 4096}}}}
        dsl-expression {:output-type :clj
                        :rules [['len '(int32be 8)]
                                ['data [['dst '(eth-mac-addr-str 16)]
                                        ['src '(eth-mac-addr-str 22)]
                                        ['data [['src '(ipv4-addr-str 42)]
                                                ['dst '(ipv4-addr-str 46)]
                                                ['data [['src '(int16 50)]
                                                        ['dst '(int16 52)]]]]]]]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (map? result))
    (is (= expected result))))

(deftest byte-array-packet-to-nested-json-str-test
  (let [expected (str "{\"len\":58,"
                       "\"data\":{\"dst\":\"FF:FE:FD:F2:F1:F0\","
                                 "\"src\":\"01:02:03:04:05:06\","
                                 "\"data\":{\"src\":\"1.2.3.4\","
                                           "\"dst\":\"252.253.254.255\","
                                           "\"data\":{\"src\":2048,"
                                                     "\"dst\":4096}}}}")
        dsl-expression {:output-type :json-str
                        :rules [['len '(int32be 8)]
                                ['data [['dst '(eth-mac-addr-str 16) :string]
                                        ['src '(eth-mac-addr-str 22) :string]
                                        ['data [['src '(ipv4-addr-str 42) :string]
                                                ['dst '(ipv4-addr-str 46) :string]
                                                ['data [['src '(int16 50)]
                                                        ['dst '(int16 52)]]]]]]]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (= expected (str result)))))

(deftest nested-udp-byte-array-packet-test-1
  (let [expected {"len" 58,
                  "data" {"dst" "FF:FE:FD:F2:F1:F0",
                          "data" {"proto-id" 17
                                  "src" "1.2.3.4",
                                  "data" {"src" 2048
                                          "dst" 4096}}}}
        dsl-expression {:output-type :clj
                        :rules [['len '(int32be 8)]
                                ['data [['dst '(eth-mac-addr-str 16)]
                                        ['data [['proto-id '(int8 39)]
                                                ['src '(ipv4-addr-str 42)]
                                                ['data [['src '(int16 50)]
                                                        ['dst '(int16 52)]]]]]]]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-byte-array-test-data)]
    (is (map? result))
    (is (= expected result))))

(deftest nested-tcp-byte-array-packet-test-1
  (let [expected {"len" 58,
                  "data" {"dst" "FF:FE:FD:F2:F1:F0",
                          "data" {"proto-id" 6
                                  "src" "127.0.0.1",
                                  "data" {"src" 55522
                                          "dst" 61481
                                          "flags" 0x18
                                          "seq-no" 4109031044
                                          "ack-no" 3662655102}}}}
        dsl-expression {:output-type :clj
                        :rules [['len '(int32be 8)]
                                ['data [['dst '(eth-mac-addr-str 16)]
                                        ['data [['proto-id '(int8 39)]
                                                ['src '(ipv4-addr-str 42)]
                                                ['data [['src '(int16 50)]
                                                        ['dst '(int16 52)]
                                                        ['flags '(int8 63)]
                                                        ['seq-no '(int32 54)]
                                                        ['ack-no '(int32 58)]]]]]]]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-tcp-byte-array-test-data)]
    (is (map? result))
    (is (= expected result))))

(deftest nested-udp-and-tcp-byte-array-packet-clj-map-test-1
  (let [expected-udp {"len" 58,
                      "data" {"dst" "FF:FE:FD:F2:F1:F0",
                              "data" {"proto-id" 17
                                      "src" "1.2.3.4",
                                      "data" {"src" 2048
                                              "dst" 4096}}}}
        expected-tcp {"len" 58,
                      "data" {"dst" "FF:FE:FD:F2:F1:F0",
                              "data" {"proto-id" 6
                                      "src" "127.0.0.1",
                                      "data" {"src" 55522
                                              "dst" 61481
                                              "flags" 0x18
                                              "seq-no" 4109031044
                                              "ack-no" 3662655102}}}}
        dsl-expression {:output-type :clj
                        :rules [['len '(int32be 8)]
                                ['data [['dst '(eth-mac-addr-str 16)]
                                        ['data [['proto-id '(int8 39)]
                                                ['src '(ipv4-addr-str 42)]
                                                ['data ['(= 17 __2_proto-id) [['src '(int16 50)]
                                                                              ['dst '(int16 52)]]
                                                        '(= 6 __2_proto-id) [['src '(int16 50)]
                                                                             ['dst '(int16 52)]
                                                                             ['flags '(int8 63)]
                                                                             ['seq-no '(int32 54)]
                                                                             ['ack-no '(int32 58)]]
                                                        :default '(str "Unsupported protocol: " __2_proto-id)]]]]]]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result-udp (data-processing-fn pcap-byte-array-test-data) 
        result-tcp (data-processing-fn pcap-tcp-byte-array-test-data)]
    (is (map? result-udp))
    (is (= expected-udp result-udp))  
    (is (map? result-tcp))
    (is (= expected-tcp result-tcp))))

(deftest nested-udp-and-tcp-byte-array-packet-java-map-test-1
  (let [expected-udp {"len" 58,
                      "data" {"dst" "FF:FE:FD:F2:F1:F0",
                              "data" {"proto-id" 17
                                      "src" "1.2.3.4",
                                      "data" {"src" 2048
                                              "dst" 4096}}}}
        expected-tcp {"len" 58,
                      "data" {"dst" "FF:FE:FD:F2:F1:F0",
                              "data" {"proto-id" 6
                                      "src" "127.0.0.1",
                                      "data" {"src" 55522
                                              "dst" 61481
                                              "flags" 0x18
                                              "seq-no" 4109031044
                                              "ack-no" 3662655102}}}}
        dsl-expression {:output-type :java
                        :rules [['len '(int32be 8)]
                                ['data [['dst '(eth-mac-addr-str 16)]
                                        ['data [['proto-id '(int8 39)]
                                                ['src '(ipv4-addr-str 42)]
                                                ['data ['(= 17 __2_proto-id) [['src '(int16 50)]
                                                                              ['dst '(int16 52)]]
                                                        '(= 6 __2_proto-id) [['src '(int16 50)]
                                                                             ['dst '(int16 52)]
                                                                             ['flags '(int8 63)]
                                                                             ['seq-no '(int32 54)]
                                                                             ['ack-no '(int32 58)]]
                                                        :default '(str "Unsupported protocol: " __2_proto-id)]]]]]]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result-udp (data-processing-fn pcap-byte-array-test-data)
        result-tcp (data-processing-fn pcap-tcp-byte-array-test-data)]
    (is (instance? Map result-udp))
    (is (= expected-udp result-udp))
    (is (instance? Map result-tcp))
    (is (= expected-tcp result-tcp))))



;;;
;;; Tests for processing nested sequences.
;;;
(deftest pcap-file-nested-packet-sequence-processing-test-1
  (let [pcap-raw-data (Files/readAllBytes (Paths/get "test/data/pcap_three_packets_icmp_dns_http.pcap" (into-array [""])))
        expected {"magic-number" 0xa1b2c3d4
                  "snapshot-len" 262144
                  "packets" [{"capture-length" 98, "packet-length" 98, "__offset-increment" 114}
                             {"capture-length" 70, "packet-length" 70, "__offset-increment" 86}
                             {"capture-length" 74, "packet-length" 74, "__offset-increment" 90}] }
        dsl-expression {:output-type :clj
                        :rules [['magic-number '(int32be 0)]
                                ['snapshot-len '(int32be 16)]
                                ['packets
                                 '([capture-length (int32be (+ offset 8))]
                                   [packet-length (int32be (+ offset 12))]
                                   [__offset-increment (+ 16 __1_capture-length)])
                                 {:initial-offset 24}]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-raw-data)]
    (is (map? result))
    (is (= expected result))))

(deftest pcap-file-nested-packet-sequence-processing-with-cond-test-1
  (let [pcap-raw-data (Files/readAllBytes (Paths/get "test/data/pcap_three_packets_icmp_dns_http.pcap" (into-array [""])))
        expected {"magic-number" 2712847316,
                  "snapshot-len" 262144,
                  "packets" [{"capture-length" 98, "packet-length" 98, "__offset-increment" 114,
                              "data" {"eth-dst" "E8:DE:27:59:0E:AD", "eth-src" "0E:AD:B4:74:9F:FA",
                                      "data" {"proto-id" 1, "ip-dst" "10.0.0.230", "ip-src" "149.20.4.69",
                                              "data" {"type" "Echo Request"}}}}
                             {"capture-length" 70, "packet-length" 70, "__offset-increment" 86,
                              "data" {"eth-dst" "E8:DE:27:59:0E:AD", "eth-src" "0E:AD:B4:74:9F:FA",
                                      "data" {"proto-id" 17, "ip-dst" "10.0.0.230", "ip-src" "10.0.0.1",
                                              "data" {"dst" 53, "src" 43471}}}}
                             {"capture-length" 74, "packet-length" 74, "__offset-increment" 90,
                              "data" {"eth-dst" "E8:DE:27:59:0E:AD", "eth-src" "0E:AD:B4:74:9F:FA",
                                      "data" {"proto-id" 6, "ip-dst" "10.0.0.230", "ip-src" "199.204.44.194",
                                              "data" {"dst" 80, "src" 49526, "flags" 74, "ack-no" 0, "seq-no" 1034137073}}}}]}
        dsl-expression {:output-type :clj
                        :rules [['magic-number '(int32be 0)]
                                ['snapshot-len '(int32be 16)]
                                ['packets
                                 '([capture-length (int32be (+ offset 8))]
                                   [packet-length (int32be (+ offset 12))]
                                   [__offset-increment (+ 16 __1_capture-length)]
                                   [data [[eth-dst (eth-mac-addr-str (+ offset 16))]
                                          [eth-src (eth-mac-addr-str (+ offset 20))]
                                          [data [[proto-id (int8 (+ offset 39))]
                                                 [ip-dst (ipv4-addr-str (+ offset 42))]
                                                 [ip-src (ipv4-addr-str (+ offset 46))]
                                                 [data [(= __3_proto-id 1) [[type (condp = (int8 (+ offset 50))
                                                                                    0 "Echo Reply"
                                                                                    3 "Destination Unreachable"
                                                                                    8 "Echo Request")]]
                                                        (= __3_proto-id 6) [[dst (int16 (+ offset 52))]
                                                                            [src (int16 (+ offset 50))]
                                                                            [flags (int8 (+ offset 8))]
                                                                            [ack-no (int32 (+ offset 58))]
                                                                            [seq-no (int32 (+ offset 54))]]
                                                        (= __3_proto-id 17) [[dst (int16 (+ offset 52))]
                                                                            [src (int16 (+ offset 50))]]]]]]]])
                                 {:initial-offset 24}]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-raw-data)]
    (is (map? result))
    (is (= expected result))))

(deftest pcap-file-nested-packet-sequence-processing-to-java-map-test-1
  (let [pcap-raw-data (Files/readAllBytes (Paths/get "test/data/pcap_three_packets_icmp_dns_http.pcap" (into-array [""])))
        expected {"magic-number" 0xa1b2c3d4
                  "snapshot-len" 262144
                  "packets" [{"capture-length" 98, "packet-length" 98, "__offset-increment" 114}
                             {"capture-length" 70, "packet-length" 70, "__offset-increment" 86}
                             {"capture-length" 74, "packet-length" 74, "__offset-increment" 90}] }
        dsl-expression {:output-type :java
                        :rules [['magic-number '(int32be 0)]
                                ['snapshot-len '(int32be 16)]
                                ['packets
                                 '([capture-length (int32be (+ offset 8))]
                                   [packet-length (int32be (+ offset 12))]
                                   [__offset-increment (+ 16 __1_capture-length)])
                                 {:initial-offset 24}]]}
        data-processing-fn (create-proc-fn dsl-expression)
        result (data-processing-fn pcap-raw-data)]
    (is (instance? java.util.HashMap result))
    (is (instance? java.util.ArrayList (.get result "packets")))
    (is (= expected result))))

