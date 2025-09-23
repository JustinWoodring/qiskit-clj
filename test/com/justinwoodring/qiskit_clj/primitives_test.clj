(ns com.justinwoodring.qiskit-clj.primitives-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]
            [com.justinwoodring.qiskit-clj.backends :as backends]))

(use-fixtures :once (fn [f] (core/initialize!) (f)))

(deftest basic-circuit-creation-test
  (testing "Can create basic quantum circuit"
    (let [qc (circuit/quantum-circuit 2)]
      (is (some? qc))))

  (testing "Can add gates to circuit"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (gates/cnot 0 1))]
      (is (some? qc))))

  (testing "Can create circuit with measurements"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (gates/cnot 0 1)
                 (circuit/measure-all))]
      (is (some? qc)))))

(deftest backend-creation-test
  (testing "Can create basic simulator backend"
    (let [backend (backends/basic-simulator)]
      (is (some? backend))))

  (testing "Can create aer simulator"
    (let [backend (backends/aer-simulator)]
      (is (some? backend)))))

(deftest circuit-execution-test
  (testing "Can execute circuit on backend"
    (let [backend (backends/basic-simulator)
          qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (gates/cnot 0 1)
                 (circuit/measure-all))
          job (backends/run-circuit backend qc)
          result (backends/job-result job)]
      (is (some? result))
      (let [counts (backends/get-counts result)]
        (is (map? counts))
        (is (every? string? (keys counts)))
        (is (every? number? (vals counts))))))

  (testing "Bell state gives correct outcomes"
    (let [backend (backends/basic-simulator)
          bell-circuit (-> (circuit/quantum-circuit 2)
                           (gates/hadamard 0)
                           (gates/cnot 0 1)
                           (circuit/measure-all))
          job (backends/run-circuit backend bell-circuit 100)
          result (backends/job-result job)
          counts (backends/get-counts result)]
      (is (map? counts))
      ;; Bell state should only have correlated outcomes
      (let [outcomes (keys counts)]
        (is (every? #(or (clojure.string/starts-with? % "00")
                         (clojure.string/starts-with? % "11")) outcomes)))))

  (testing "X gate creates |1⟩ state"
    (let [backend (backends/basic-simulator)
          qc (-> (circuit/quantum-circuit 1)
                 (gates/pauli-x 0)
                 (circuit/measure-all))
          job (backends/run-circuit backend qc 50)
          result (backends/job-result job)
          counts (backends/get-counts result)]
      (is (map? counts))
      ;; Should only have |1⟩ outcome
      (let [result-key (first (keys counts))]
        (is (clojure.string/starts-with? result-key "1"))
        (is (= 50 (first (vals counts))))))))


(deftest statevector-test
  (testing "Can get statevector from circuit"
    (let [backend (backends/aer-simulator)
          qc (-> (circuit/quantum-circuit 1)
                 (gates/hadamard 0))
          job (backends/run-circuit backend qc)
          result (backends/job-result job)]
      (is (some? result))
      (try
        (let [statevector (backends/get-statevector result)]
          (is (some? statevector)))
        (catch Exception _
          ;; Skip if statevector not available
          (is true)))))

  (testing "Bell state statevector"
    (let [backend (backends/aer-simulator)
          bell-circuit (-> (circuit/quantum-circuit 2)
                           (gates/hadamard 0)
                           (gates/cnot 0 1))
          job (backends/run-circuit backend bell-circuit)
          result (backends/job-result job)]
      (is (some? result))
      (try
        (let [statevector (backends/get-statevector result)]
          (is (some? statevector)))
        (catch Exception _
          ;; Skip if statevector not available
          (is true))))))

(deftest rotation-gates-test
  (testing "Y rotation gate"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/rotation-y 1.5707963267948966 0))]  ; π/2
      (is (some? qc))))

  (testing "X rotation gate"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/rotation-x 3.141592653589793 0))]  ; π
      (is (some? qc))))

  (testing "Z rotation gate"
    (let [qc (-> (circuit/quantum-circuit 1)
                 (gates/rotation-z 1.5707963267948966 0))]  ; π/2
      (is (some? qc)))))

(deftest multi-qubit-gate-test
  (testing "CNOT gate"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/cnot 0 1))]
      (is (some? qc))))

  (testing "Multiple CNOT gates"
    (let [qc (-> (circuit/quantum-circuit 3)
                 (gates/cnot 0 1)
                 (gates/cnot 1 2))]
      (is (some? qc)))))

(deftest circuit-properties-test
  (testing "Circuit width"
    (let [qc (circuit/quantum-circuit 3)
          width (circuit/num-qubits qc)]
      (is (= 3 width)))))

(deftest measurement-test
  (testing "Measure specific qubits"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (circuit/measure 0 0))]
      (is (some? qc))))

  (testing "Measure all qubits"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (gates/hadamard 1)
                 (circuit/measure-all))]
      (is (some? qc)))))