;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Utility functions for working with processing fns."}
  dsbdp.processing-fn-utils
  (:require
    [clojure.walk :refer :all]
    [clojure.pprint :refer :all]))

(defn create-proc-fn-vec-from-template
  "Create a vector with n processing functions based on the two given \"template\" functions fn-1 and fn-n.
   fn-1 is the function that will be put as first function in the resulting vector.
   Subsequently, fn-n will be repeated n-1 times to fill the vector with n instances.
   All occurrences of :_idx_ in the supplied functions will be replaced with the numerical index value of the corresponding function in the resulting vector."
  [fn-1 fn-n n]
  (loop [fns (prewalk-replace {:_idx_ 0} [fn-1])]
    (if (< (count fns) n)
      (recur (conj fns (prewalk-replace {:_idx_ (count fns)} fn-n)))
      (do
        (println "proc-fns-full:" fns)
        (println "proc-fns-short:" (.replaceAll (str fns) "(?<=\\()([a-zA-Z\\.\\-]++/)" ""))
        (println "proc-fns-pretty:\n" (.replaceAll (str (with-out-str (pprint fns))) "(?<=\\()([a-zA-Z\\.\\-]++/)" ""))
        (vec
          (map eval fns))))))

(defn combine-proc-fns
  "Based on the given vector of processing functions, combine the functions starting at start-idx, inclusive, up to end-idx, non inclusive, into a single combined function."
  [fn-vec start-idx end-idx]
  (fn [in out]
    (loop [fns (subvec fn-vec start-idx end-idx) o out]
      (if (empty? fns)
        o
        (recur (rest fns) ((first fns) in o))))))

(defn combine-proc-fns-vec
  "Map a vector of processing functions, fn-vec, to a vector of combined functions.
   The mapping definition fn-mapping, is a vector of integer values that define how many processing functions are combined in each combined function.
   For a vector of processing functions [a b c d e f] a mapping definition [1 2 3] will result in the following association of processing rules to combined functions f_x: [f_1(a), f_2(b, c), f_3(d e f)]."
  [fn-mapping fn-vec]
  (reduce
    (fn [v m]
      (let [start-idx (reduce + (subvec fn-mapping 0 (count v)))]
        (conj v (combine-proc-fns fn-vec start-idx (+ start-idx m)))))
    (let [f (combine-proc-fns fn-vec 0 (first fn-mapping))]
      [f])
    (rest fn-mapping)))

(defn calculate-distribution-mapping
  "Based on an input sequence, in-seq, and a vector of ratios, calculate a mapping such that the input sequence will be distributed according to the given ratios.
   The resulting mapping can be used, e.g., with combine-proc-fns-vec."
  ([in-seq ratios]
    (calculate-distribution-mapping in-seq ratios {}))
  ([in-seq ratios opts]
    (let [mapping (reduce
                    (fn [v r]
                      (conj v (int (* (count in-seq) r))))
                    []
                    ratios)]
      (loop [m mapping
             idx (if (:fill-end opts)
                   (dec (count ratios))
                   0)]
        (if (= (count in-seq) (reduce + m))
          m
          (recur
            (assoc
              m
              idx
              (inc (m idx)))
            (if (:fill-end opts)
              (dec idx)
              (inc idx))))))))

