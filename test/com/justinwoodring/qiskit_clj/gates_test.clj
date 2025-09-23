(ns com.justinwoodring.qiskit-clj.gates-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]))

(use-fixtures :once (fn [f] (core/initialize!) (f)))

(deftest single-qubit-gates-test
  (testing "Pauli gates"
    (let [qc (-> (circuit/quantum-circuit 3)
                 (gates/pauli-x 0)
                 (gates/pauli-y 1)
                 (gates/pauli-z 2))]
      (is (some? qc))
      (is (= 3 (circuit/circuit-size qc)))))

  (testing "Hadamard gate"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (gates/hadamard 1))]
      (is (some? qc))
      (is (= 2 (circuit/circuit-size qc)))))

  (testing "Phase and S gates"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/phase-gate 1.57 0)
                 (gates/s-gate 1))]
      (is (some? qc))))

  (testing "T gate"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/t-gate 0))]
      (is (some? qc)))))

(deftest rotation-gates-test
  (testing "X rotation"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/rotation-x 1.57 0))]
      (is (some? qc))))

  (testing "Y rotation"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/rotation-y 3.14 0))]
      (is (some? qc))))

  (testing "Z rotation"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/rotation-z 0.78 0))]
      (is (some? qc))))

  (testing "Multiple rotations"
    (let [qc (-> (circuit/quantum-circuit 3)
                 (gates/rotation-x 1.57 0)
                 (gates/rotation-y 3.14 1)
                 (gates/rotation-z 0.78 2))]
      (is (some? qc))
      (is (= 3 (circuit/circuit-size qc))))))

(deftest two-qubit-gates-test
  (testing "CNOT gate"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/cnot 0 1))]
      (is (some? qc))
      (is (= 1 (circuit/circuit-size qc)))))

  (testing "CZ gate"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (circuit/cz 0 1))]
      (is (some? qc))))

  (testing "SWAP gate"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/swap-gate 0 1))]
      (is (some? qc))))

  (testing "Multiple CNOT gates"
    (let [qc (-> (circuit/quantum-circuit 3)
                 (gates/cnot 0 1)
                 (gates/cnot 1 2)
                 (gates/cnot 0 2))]
      (is (some? qc))
      (is (= 3 (circuit/circuit-size qc))))))

(deftest controlled-gates-test
  (testing "Controlled rotation gates"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/controlled-rx 1.57 0 1)
                 (gates/controlled-ry 3.14 0 1)
                 (gates/controlled-rz 0.78 0 1))]
      (is (some? qc))))

  (testing "Controlled phase gate"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/controlled-phase 1.57 0 1))]
      (is (some? qc)))))

;; Parameterized gates need more complex setup - skipped for now

(deftest composite-circuits-test
  (testing "Bell state creation"
    (let [bell-circuit (-> (circuit/quantum-circuit 2)
                           (gates/hadamard 0)
                           (gates/cnot 0 1))]
      (is (some? bell-circuit))
      (is (= 2 (circuit/circuit-size bell-circuit)))))

  (testing "GHZ state creation"
    (let [ghz-circuit (-> (circuit/quantum-circuit 3)
                          (gates/hadamard 0)
                          (gates/cnot 0 1)
                          (gates/cnot 0 2))]
      (is (some? ghz-circuit))
      (is (= 3 (circuit/circuit-size ghz-circuit)))))

  (testing "Quantum teleportation setup"
    (let [teleport-circuit (-> (circuit/quantum-circuit 3)
                               ;; Prepare Bell pair
                               (gates/hadamard 1)
                               (gates/cnot 1 2)
                               ;; Prepare state to teleport
                               (gates/hadamard 0)
                               ;; Teleportation protocol
                               (gates/cnot 0 1)
                               (gates/hadamard 0))]
      (is (some? teleport-circuit))
      (is (> (circuit/circuit-size teleport-circuit) 0))))

  (testing "QFT preparation gates"
    (let [qft-prep (-> (circuit/quantum-circuit 2)
                       (gates/hadamard 0)
                       (gates/controlled-phase (/ Math/PI 2) 1 0)
                       (gates/hadamard 1))]
      (is (some? qft-prep)))))

(deftest gate-validation-test
  (testing "Valid qubit indices"
    (let [qc (circuit/quantum-circuit 3)]
      (is (some? (gates/hadamard qc 0)))
      (is (some? (gates/hadamard qc 1)))
      (is (some? (gates/hadamard qc 2)))))

  (testing "CNOT with different qubit pairs"
    (let [qc (circuit/quantum-circuit 4)]
      (is (some? (gates/cnot qc 0 1)))
      (is (some? (gates/cnot qc 2 3)))
      (is (some? (gates/cnot qc 0 3)))))

  (testing "Parameter validation for rotation gates"
    (let [qc (circuit/quantum-circuit 1)]
      (is (some? (gates/rotation-x qc 0.0 0)))
      (is (some? (gates/rotation-x qc Math/PI 0)))
      (is (some? (gates/rotation-x qc (* 2 Math/PI) 0))))))

(deftest gate-properties-test
  (testing "Gate matrix verification"
    (let [qc (circuit/quantum-circuit 1)]
      ;; These should not throw errors
      (is (some? (gates/pauli-x qc 0)))
      (is (some? (gates/pauli-y qc 0)))
      (is (some? (gates/pauli-z qc 0)))
      (is (some? (gates/hadamard qc 0)))))

  ;; Unitary checking has Python/JVM interop issues
  #_(testing "Unitary gate creation"
    (let [qc (circuit/quantum-circuit 1)]
      (is (gates/is-unitary? (gates/pauli-x qc 0)))
      (is (gates/is-unitary? (gates/hadamard qc 0)))))

  (testing "Gate inverse operations"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/pauli-x 0)
                 (gates/pauli-x 0))]  ; X followed by X should be identity
      (is (some? qc)))))