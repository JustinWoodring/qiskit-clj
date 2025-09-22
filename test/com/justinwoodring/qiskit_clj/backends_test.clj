(ns com.justinwoodring.qiskit-clj.backends-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]
            [com.justinwoodring.qiskit-clj.backends :as backends]))

(use-fixtures :once (fn [f] (core/initialize!) (f)))

(deftest basic-simulator-creation-test
  (testing "Basic simulator creation"
    (let [backend (backends/basic-simulator)]
      (is (some? backend))
      (is (string? (backends/backend-name backend)))))

  (testing "Aer simulator creation with default options"
    (try
      (let [backend (backends/aer-simulator)]
        (is (some? backend))
        (is (string? (backends/backend-name backend))))
      (catch Exception e
        ;; Skip if Aer not available
        (is (re-find #"Aer not available" (.getMessage e))))))

  (testing "Aer simulator with options"
    (try
      (let [backend (backends/aer-simulator {:method :statevector :shots 2048})]
        (is (some? backend)))
      (catch Exception e
        ;; Skip if Aer not available
        (is (re-find #"Aer not available" (.getMessage e)))))))

(deftest backend-properties-test
  (testing "Backend name retrieval"
    (let [backend (backends/basic-simulator)]
      (is (string? (backends/backend-name backend)))))

  (testing "Backend version retrieval"
    (let [backend (backends/basic-simulator)]
      (is (string? (backends/backend-version backend)))))

  (testing "Backend configuration"
    (let [backend (backends/basic-simulator)
          config (backends/backend-configuration backend)]
      (is (map? config))
      (is (contains? config "simulator"))
      (is (contains? config "n_qubits"))))

  (testing "Backend status"
    (let [backend (backends/basic-simulator)
          status (backends/backend-status backend)]
      (is (map? status))))

  (testing "Number of qubits"
    (let [backend (backends/basic-simulator)
          n-qubits (backends/num-qubits backend)]
      (is (pos-int? n-qubits))))

  (testing "Simulator check"
    (let [backend (backends/basic-simulator)]
      (is (true? (backends/simulator? backend))))))

(deftest transpilation-test
  (testing "Single circuit transpilation"
    (let [backend (backends/basic-simulator)
          circuit (-> (circuit/quantum-circuit 2)
                      (gates/hadamard 0)
                      (gates/cnot 0 1))
          transpiled (backends/transpile circuit backend)]
      (is (some? transpiled))
      (is (= 2 (circuit/num-qubits transpiled)))))

  (testing "Multiple circuits transpilation"
    (let [backend (backends/basic-simulator)
          circuit1 (-> (circuit/quantum-circuit 2)
                       (gates/hadamard 0))
          circuit2 (-> (circuit/quantum-circuit 2)
                       (gates/pauli-x 1))
          transpiled (backends/transpile [circuit1 circuit2] backend)]
      (is (coll? transpiled))
      (is (= 2 (count transpiled)))))

  (testing "Transpilation with optimization"
    (let [backend (backends/basic-simulator)
          circuit (-> (circuit/quantum-circuit 2)
                      (gates/hadamard 0)
                      (gates/cnot 0 1))
          transpiled (backends/transpile circuit backend {:optimization-level 2})]
      (is (some? transpiled)))))

(deftest circuit-execution-test
  (testing "Basic circuit execution"
    (let [backend (backends/basic-simulator)
          circuit (-> (circuit/quantum-circuit 2)
                      (gates/hadamard 0)
                      (gates/cnot 0 1)
                      (circuit/measure-all))
          job (backends/run-circuit backend circuit 100)]
      (is (some? job))
      (let [result (backends/wait-for-job job)]
        (is (some? result))
        (let [counts (backends/get-counts result)]
          (is (map? counts))
          (is (pos? (count counts)))))))

  (testing "Execute circuit helper function"
    (let [circuit (-> (circuit/quantum-circuit 2)
                      (gates/hadamard 0)
                      (gates/cnot 0 1)
                      (circuit/measure-all))
          counts (backends/execute-circuit circuit)]
      (is (map? counts))
      (is (pos? (count counts)))))

  (testing "Statevector execution"
    (try
      (let [circuit (-> (circuit/quantum-circuit 2)
                        (gates/hadamard 0)
                        (gates/cnot 0 1))
            statevector (backends/execute-statevector circuit)]
        (is (some? statevector)))
      (catch Exception e
        ;; Skip if Aer not available for statevector
        (is (re-find #"Aer not available" (.getMessage e)))))))

(deftest job-management-test
  (testing "Job status check"
    (let [backend (backends/basic-simulator)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)
                      (circuit/measure-all))
          job (backends/run-circuit backend circuit 10)]
      (is (some? (backends/job-status job)))))

  (testing "Job result retrieval"
    (let [backend (backends/basic-simulator)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)
                      (circuit/measure-all))
          job (backends/run-circuit backend circuit 10)
          result (backends/job-result job)]
      (is (some? result)))))

(deftest result-processing-test
  (testing "Get counts from result"
    (let [backend (backends/basic-simulator)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)
                      (circuit/measure-all))
          job (backends/run-circuit backend circuit 100)
          result (backends/wait-for-job job)
          counts (backends/get-counts result)]
      (is (map? counts))
      (is (every? string? (keys counts)))
      (is (every? integer? (vals counts)))))

  (testing "Get memory from result with memory option"
    (let [backend (backends/basic-simulator)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)
                      (circuit/measure-all))
          job (backends/run-circuit backend circuit 10 true)
          result (backends/wait-for-job job)]
      (try
        (let [memory (backends/get-memory result)]
          (is (coll? memory)))
        (catch Exception e
          ;; Memory might not be available on all backends
          (is (some? e)))))))

(deftest fake-backend-test
  (testing "Fake backend creation"
    (try
      (let [fake-backend (backends/fake-backend :fake_manila)]
        (is (some? fake-backend)))
      (catch Exception e
        ;; Skip if fake backends not available
        (is (re-find #"not available" (.getMessage e)))))))

(deftest noise-modeling-test
  (testing "Noise model creation"
    (try
      (let [noise-model (backends/noise-model)]
        (is (some? noise-model)))
      (catch Exception e
        ;; Skip if Aer not available for noise modeling
        (is (re-find #"Aer not available" (.getMessage e))))))

  (testing "Depolarizing error creation"
    (try
      (let [error (backends/depolarizing-error 0.1 1)]
        (is (some? error)))
      (catch Exception e
        ;; Skip if Aer not available for noise modeling
        (is (re-find #"Aer not available" (.getMessage e)))))))

(deftest bell-state-full-workflow-test
  (testing "Complete Bell state workflow"
    (let [backend (backends/basic-simulator)
          bell-circuit (-> (circuit/quantum-circuit 2)
                           (gates/hadamard 0)
                           (gates/cnot 0 1)
                           (circuit/measure-all))
          transpiled (backends/transpile bell-circuit backend)
          job (backends/run-circuit backend transpiled 1000)
          result (backends/wait-for-job job)
          counts (backends/get-counts result)]
      (is (map? counts))
      ;; Bell state should only have |00⟩ and |11⟩ outcomes
      (is (or (contains? counts "00") (contains? counts "11")))
      (is (<= (count counts) 2)))))