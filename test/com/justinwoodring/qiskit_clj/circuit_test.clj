(ns com.justinwoodring.qiskit-clj.circuit-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]))

(use-fixtures :once (fn [f] (core/initialize!) (f)))

(deftest quantum-circuit-creation-test
  (testing "Basic circuit creation"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= 2 (circuit/num-qubits qc)))
      (is (= 2 (circuit/num-clbits qc)))))

  (testing "Circuit with different classical bits"
    (let [qc (circuit/quantum-circuit 3 2)]
      (is (= 3 (circuit/num-qubits qc)))
      (is (= 2 (circuit/num-clbits qc)))))

  (testing "Named circuit"
    (let [qc (circuit/quantum-circuit 2 2 "test-circuit")]
      (is (= 2 (circuit/num-qubits qc))))))

(deftest circuit-properties-test
  (testing "Empty circuit properties"
    (let [qc (circuit/quantum-circuit 3)]
      (is (= 3 (circuit/num-qubits qc)))
      (is (= 3 (circuit/num-clbits qc)))
      (is (= 0 (circuit/circuit-depth qc)))
      (is (= 0 (circuit/circuit-size qc)))
      (is (= 6 (circuit/circuit-width qc)))))

  (testing "Circuit with gates"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (circuit/h 0)
                 (circuit/cx 0 1))]
      (is (= 2 (circuit/circuit-size qc)))
      (is (> (circuit/circuit-depth qc) 0)))))

(deftest basic-gates-test
  (testing "Hadamard gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (circuit/h qc 0)))
      (is (= qc (circuit/h qc [0 1])))))

  (testing "Pauli gates"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (circuit/x qc 0)))
      (is (= qc (circuit/y qc 1)))
      (is (= qc (circuit/z qc 0)))))

  (testing "CNOT gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (circuit/cx qc 0 1)))))

  (testing "Rotation gates"
    (let [qc (circuit/quantum-circuit 1)
          theta (/ Math/PI 4)]
      (is (= qc (circuit/rx qc theta 0)))
      (is (= qc (circuit/ry qc theta 0)))
      (is (= qc (circuit/rz qc theta 0))))))

(deftest measurement-test
  (testing "Single measurement"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (circuit/measure qc 0 0)))))

  (testing "Measure all"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (circuit/measure-all qc))))))

(deftest circuit-operations-test
  (testing "Barrier"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (circuit/barrier qc)))
      (is (= qc (circuit/barrier qc [0])))))

  (testing "Reset"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (circuit/reset qc 0)))
      (is (= qc (circuit/reset qc [0 1])))))

  (testing "Copy circuit"
    (let [qc (circuit/quantum-circuit 2)
          qc-copy (circuit/copy-circuit qc)]
      (is (= (circuit/num-qubits qc) (circuit/num-qubits qc-copy))))))

(deftest circuit-composition-test
  (testing "Circuit composition"
    (let [qc1 (-> (circuit/quantum-circuit 2)
                  (circuit/h 0))
          qc2 (-> (circuit/quantum-circuit 2)
                  (circuit/x 1))
          composed (circuit/compose qc1 qc2)]
      (is (= 2 (circuit/num-qubits composed))))))

(deftest circuit-drawing-test
  (testing "Circuit drawing"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (circuit/h 0)
                 (circuit/cx 0 1))
          drawing (circuit/draw qc)]
      (is (string? drawing)))))

(deftest bell-state-circuit-test
  (testing "Bell state circuit creation and properties"
    (let [bell-circuit (-> (circuit/quantum-circuit 2)
                           (circuit/h 0)
                           (circuit/cx 0 1))]
      (is (= 2 (circuit/num-qubits bell-circuit)))
      (is (= 2 (circuit/circuit-size bell-circuit))))))

(deftest threading-macro-test
  (testing "Circuit construction with threading macros"
    (let [qc (-> (circuit/quantum-circuit 3)
                 (circuit/h 0)
                 (circuit/cx 0 1)
                 (circuit/cx 1 2)
                 (circuit/barrier)
                 (circuit/measure-all))]
      (is (= 3 (circuit/num-qubits qc)))
      (is (> (circuit/circuit-size qc) 3)))))