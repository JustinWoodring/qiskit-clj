(ns com.justinwoodring.qiskit-clj.gates-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]))

(use-fixtures :once (fn [f] (core/initialize!) (f)))

(deftest basic-single-qubit-gates-test
  (testing "Hadamard gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/hadamard qc 0)))
      (is (= qc (gates/hadamard qc [0 1])))))

  (testing "Pauli-X gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/pauli-x qc 0)))
      (is (= qc (gates/pauli-x qc [0 1])))))

  (testing "Pauli-Y gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/pauli-y qc 0)))
      (is (= qc (gates/pauli-y qc [0 1])))))

  (testing "Pauli-Z gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/pauli-z qc 0)))
      (is (= qc (gates/pauli-z qc [0 1])))))

  (testing "S gate"
    (let [qc (circuit/quantum-circuit 1)]
      (is (= qc (gates/s-gate qc 0)))))

  (testing "S-dagger gate"
    (let [qc (circuit/quantum-circuit 1)]
      (is (= qc (gates/s-dagger qc 0)))))

  (testing "T gate"
    (let [qc (circuit/quantum-circuit 1)]
      (is (= qc (gates/t-gate qc 0)))))

  (testing "T-dagger gate"
    (let [qc (circuit/quantum-circuit 1)]
      (is (= qc (gates/t-dagger qc 0))))))

(deftest rotation-gates-test
  (testing "RX rotation"
    (let [qc (circuit/quantum-circuit 1)
          theta (/ Math/PI 4)]
      (is (= qc (gates/rotation-x qc theta 0)))))

  (testing "RY rotation"
    (let [qc (circuit/quantum-circuit 1)
          theta (/ Math/PI 3)]
      (is (= qc (gates/rotation-y qc theta 0)))))

  (testing "RZ rotation"
    (let [qc (circuit/quantum-circuit 1)
          theta (/ Math/PI 2)]
      (is (= qc (gates/rotation-z qc theta 0)))))

  (testing "Phase gate"
    (let [qc (circuit/quantum-circuit 1)
          lambda (/ Math/PI 8)]
      (is (= qc (gates/phase-gate qc lambda 0)))))

  (testing "U3 gate"
    (let [qc (circuit/quantum-circuit 1)
          theta (/ Math/PI 4)
          phi (/ Math/PI 6)
          lambda (/ Math/PI 8)]
      (is (= qc (gates/u3-gate qc theta phi lambda 0)))))

  (testing "U2 gate"
    (let [qc (circuit/quantum-circuit 1)
          phi (/ Math/PI 6)
          lambda (/ Math/PI 8)]
      (is (= qc (gates/u2-gate qc phi lambda 0)))))

  (testing "U1 gate"
    (let [qc (circuit/quantum-circuit 1)
          lambda (/ Math/PI 8)]
      (is (= qc (gates/u1-gate qc lambda 0))))))

(deftest two-qubit-gates-test
  (testing "CNOT gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/cnot qc 0 1)))))

  (testing "CZ gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/controlled-z qc 0 1)))))

  (testing "CY gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/controlled-y qc 0 1)))))

  (testing "CH gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/controlled-hadamard qc 0 1)))))

  (testing "Controlled-phase gate"
    (let [qc (circuit/quantum-circuit 2)
          theta (/ Math/PI 4)]
      (is (= qc (gates/controlled-phase qc theta 0 1)))))

  (testing "CRX gate"
    (let [qc (circuit/quantum-circuit 2)
          theta (/ Math/PI 4)]
      (is (= qc (gates/controlled-rx qc theta 0 1)))))

  (testing "CRY gate"
    (let [qc (circuit/quantum-circuit 2)
          theta (/ Math/PI 4)]
      (is (= qc (gates/controlled-ry qc theta 0 1)))))

  (testing "CRZ gate"
    (let [qc (circuit/quantum-circuit 2)
          theta (/ Math/PI 4)]
      (is (= qc (gates/controlled-rz qc theta 0 1)))))

  (testing "SWAP gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/swap-gate qc 0 1)))))

  (testing "iSWAP gate"
    (let [qc (circuit/quantum-circuit 2)]
      (is (= qc (gates/iswap-gate qc 0 1))))))

(deftest multi-qubit-gates-test
  (testing "Toffoli gate (CCX)"
    (let [qc (circuit/quantum-circuit 3)]
      (is (= qc (gates/toffoli qc 0 1 2)))))

  (testing "Fredkin gate (CSWAP)"
    (let [qc (circuit/quantum-circuit 3)]
      (is (= qc (gates/fredkin qc 0 1 2)))))

  (testing "Multi-controlled X gate"
    (let [qc (circuit/quantum-circuit 4)]
      (is (= qc (gates/multi-controlled-x qc [0 1 2] 3)))))

  (testing "Multi-controlled Z gate"
    (let [qc (circuit/quantum-circuit 4)]
      (is (= qc (gates/multi-controlled-z qc [0 1 2] 3))))))

(deftest special-gates-test
  (testing "Identity gate"
    (let [qc (circuit/quantum-circuit 1)]
      (is (= qc (gates/identity-gate qc 0)))))

  (testing "Reset operation"
    (let [qc (circuit/quantum-circuit 1)]
      (is (= qc (gates/reset-gate qc 0))))))

(deftest gate-validation-test
  (testing "Invalid qubit indices should throw"
    (let [qc (circuit/quantum-circuit 2)]
      (is (thrown? Exception (gates/hadamard qc 2)))
      (is (thrown? Exception (gates/cnot qc 0 2)))
      (is (thrown? Exception (gates/cnot qc 2 1))))))

(deftest gate-chaining-test
  (testing "Gates can be chained with threading macros"
    (let [qc (-> (circuit/quantum-circuit 3)
                 (gates/hadamard 0)
                 (gates/cnot 0 1)
                 (gates/cnot 1 2)
                 (gates/pauli-x 2))]
      (is (= 3 (circuit/num-qubits qc)))
      (is (= 4 (circuit/circuit-size qc))))))

(deftest bell-state-construction-test
  (testing "Bell state circuit with gates"
    (let [bell-circuit (-> (circuit/quantum-circuit 2)
                           (gates/hadamard 0)
                           (gates/cnot 0 1))]
      (is (= 2 (circuit/num-qubits bell-circuit)))
      (is (= 2 (circuit/circuit-size bell-circuit))))))

(deftest ghz-state-construction-test
  (testing "GHZ state circuit with gates"
    (let [ghz-circuit (-> (circuit/quantum-circuit 3)
                          (gates/hadamard 0)
                          (gates/cnot 0 1)
                          (gates/cnot 1 2))]
      (is (= 3 (circuit/num-qubits ghz-circuit)))
      (is (= 3 (circuit/circuit-size ghz-circuit))))))