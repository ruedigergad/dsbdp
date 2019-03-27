;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for a Processing Loop"}
  dsbdp.test.processing-loop-tests
  (:require [clojure.test :refer :all]
            [clj-assorted-utils.util :refer :all])
  (:import (dsbdp ProcessingLoop)))

(deftest simple-processing-loop-test
  (let [flag (prepare-flag)
        proc-loop (ProcessingLoop. #(if (not (flag-set? flag))
                                      (set-flag flag)))]
    (is (not (flag-set? flag)))
    (.start proc-loop)
    (await-flag flag)
    (is (flag-set? flag))
    (.stop proc-loop)))

(deftest simple-processing-loop-interrupt-test
  (let [flag (prepare-flag)
        ex-flag (prepare-flag)
        proc-loop (ProcessingLoop. (fn []
                                     (set-flag flag)
                                     (try
                                       (sleep 10000)
                                       (catch InterruptedException e
                                         (set-flag ex-flag)))))]
    (.start proc-loop)
    (await-flag flag)
    (sleep 100)
    (.interrupt proc-loop)
    (await-flag ex-flag)
    (is (flag-set? ex-flag))))

