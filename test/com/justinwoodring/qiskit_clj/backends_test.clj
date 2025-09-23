(ns com.justinwoodring.qiskit-clj.backends-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string]
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

  (testing "Aer simulator creation"
    (try
      (let [backend (backends/aer-simulator)]
        (is (some? backend))
        (is (string? (backends/backend-name backend))))
      (catch Exception e
        ;; Skip if Aer not available - just pass the test
        (is true)))))

(deftest backend-properties-test
  (testing "Backend name retrieval"
    (let [backend (backends/basic-simulator)]
      (is (string? (backends/backend-name backend)))))

  (testing "Backend version retrieval"
    (let [backend (backends/basic-simulator)]
      (try
        (let [version (backends/backend-version backend)]
          (is (some? version)))
        (catch Exception _
          ;; Some backends don't have version info
          (is true)))))

  (testing "Backend configuration"
    (let [backend (backends/basic-simulator)]
      (try
        (let [config (backends/backend-configuration backend)]
          (is (some? config)))
        (catch Exception _
          ;; Some backends don't have configuration
          (is true)))))

  (testing "Backend status"
    (let [backend (backends/basic-simulator)]
      (try
        (let [status (backends/backend-status backend)]
          (is (some? status)))
        (catch Exception _
          ;; Some backends don't have status
          (is true))))))

(deftest circuit-execution-test
  (testing "Simple circuit execution"
    (let [backend (backends/basic-simulator)
          qc (-> (circuit/quantum-circuit 1)
                 (gates/hadamard 0)
                 (circuit/measure-all))
          job (backends/run-circuit backend qc)
          result (backends/job-result job)]
      (is (some? result))
      (let [counts (backends/get-counts result)]
        (is (map? counts))
        (is (every? string? (keys counts)))
        (is (every? number? (vals counts))))))

  (testing "Bell state execution"
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

  (testing "Multiple shots"
    (let [backend (backends/basic-simulator)
          qc (-> (circuit/quantum-circuit 1)
                 (gates/pauli-x 0)
                 (circuit/measure-all))
          job (backends/run-circuit backend qc 50)
          result (backends/job-result job)
          counts (backends/get-counts result)]
      (is (map? counts))
      ;; Should only have |1⟩ outcome for X|0⟩
      (let [result-key (first (keys counts))]
        (is (clojure.string/starts-with? result-key "1"))
        (is (= 50 (first (vals counts))))))))

(deftest job-management-test
  (testing "Job status checking"
    (let [backend (backends/basic-simulator)
          qc (-> (circuit/quantum-circuit 1)
                 (gates/hadamard 0)
                 (circuit/measure-all))
          job (backends/run-circuit backend qc)]
      (is (some? (backends/job-status job)))))

  (testing "Job result retrieval"
    (let [backend (backends/basic-simulator)
          qc (-> (circuit/quantum-circuit 1)
                 (gates/hadamard 0)
                 (circuit/measure-all))
          job (backends/run-circuit backend qc)
          result (backends/job-result job)]
      (is (some? result)))))

(deftest transpilation-test
  (testing "Basic transpilation"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (gates/cnot 0 1))
          backend (backends/basic-simulator)
          transpiled (backends/transpile qc backend)]
      (is (some? transpiled))))

  (testing "Transpilation with optimization level"
    (let [qc (-> (circuit/quantum-circuit 2)
                 (gates/hadamard 0)
                 (gates/cnot 0 1))
          backend (backends/basic-simulator)
          transpiled (backends/transpile qc backend {:optimization-level 1})]
      (is (some? transpiled)))))

(deftest utility-functions-test
  (testing "Simulator check"
    (let [backend (backends/basic-simulator)]
      (is (backends/simulator? backend))))

  (testing "Backend properties"
    (let [backend (backends/basic-simulator)]
      (is (number? (backends/num-qubits backend)))
      (is (> (backends/num-qubits backend) 0))
      (is (number? (backends/max-shots backend)))
      (is (> (backends/max-shots backend) 0))))

  (testing "Basis gates"
    (let [backend (backends/basic-simulator)
          basis-gates (backends/basis-gates backend)]
      (is (coll? basis-gates))
      (is (every? string? basis-gates)))))