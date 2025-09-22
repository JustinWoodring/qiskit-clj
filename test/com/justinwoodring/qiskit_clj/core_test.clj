(ns com.justinwoodring.qiskit-clj.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.justinwoodring.qiskit-clj.core :as core]))

(deftest initialization-test
  (testing "Python environment initialization"
    (is (nil? (core/initialize!)))
    (is (true? core/*python-initialized*))))

(deftest data-conversion-test
  (testing "Clojure to Python data conversion"
    (core/ensure-initialized!)
    (let [clj-data [1 2 3]
          py-data (core/->py clj-data)
          back-to-clj (core/->clj py-data)]
      (is (= clj-data back-to-clj))))

  (testing "Map conversion"
    (core/ensure-initialized!)
    (let [clj-map {:a 1 :b 2}
          py-dict (core/->py clj-map)
          back-to-clj (core/->clj py-dict)]
      (is (= {"a" 1 "b" 2} back-to-clj)))))

(deftest validation-test
  (testing "Qubit count validation"
    (is (= 3 (core/validate-qubit-count 3)))
    (is (thrown? Exception (core/validate-qubit-count 0)))
    (is (thrown? Exception (core/validate-qubit-count -1)))
    (is (thrown? Exception (core/validate-qubit-count 1.5))))

  (testing "Qubit index validation"
    (is (= 0 (core/validate-qubit-index 0 3)))
    (is (= 2 (core/validate-qubit-index 2 3)))
    (is (thrown? Exception (core/validate-qubit-index 3 3)))
    (is (thrown? Exception (core/validate-qubit-index -1 3)))))

(deftest error-handling-test
  (testing "Qiskit error detection"
    (let [qiskit-ex (Exception. "qiskit.circuit.exceptions.CircuitError: Invalid gate")]
      (is (true? (core/qiskit-error? qiskit-ex))))

    (let [normal-ex (Exception. "Regular error")]
      (is (false? (core/qiskit-error? normal-ex))))))

(deftest utility-functions-test
  (testing "Complex number formatting"
    (is (string? (core/format-complex 1.5)))
    (is (string? (core/format-complex (+ 1 (* 2 (Math/sqrt -1))))))
    (is (= "1.0" (core/format-complex 1.0)))
    (is (= "0.0" (core/format-complex 0.0))))

  (testing "with-qiskit macro"
    (is (nil? (core/with-qiskit nil))))

  (testing "ensure-initialized function"
    (is (nil? (core/ensure-initialized!))))

  (testing "qiskit version retrieval"
    (is (string? (core/qiskit-version)))))