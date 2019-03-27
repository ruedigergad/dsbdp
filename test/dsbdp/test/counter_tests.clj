;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Unit tests for a Counter"}
  dsbdp.test.counter-tests
  (:require [clojure.test :refer :all])
  (:import (dsbdp Counter)))

(deftest simple-counter-increment-test
  (let [cntr (Counter.)]
    (is (= 0 (.value cntr)))
    (.inc cntr)
    (is (= 1 (.value cntr)))))

(deftest simple-counter-reset-test
  (let [cntr (Counter.)]
    (is (= 0 (.value cntr)))
    (.inc cntr)
    (is (= 1 (.value cntr)))
    (.reset cntr)
    (is (= 0 (.value cntr)))
    (.inc cntr)
    (is (= 1 (.value cntr)))))

