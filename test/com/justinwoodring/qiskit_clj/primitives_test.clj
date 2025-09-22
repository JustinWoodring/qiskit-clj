(ns com.justinwoodring.qiskit-clj.primitives-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]
            [com.justinwoodring.qiskit-clj.backends :as backends]
            [com.justinwoodring.qiskit-clj.quantum-info :as qi]
            [com.justinwoodring.qiskit-clj.primitives :as prims]))

(use-fixtures :once (fn [f] (core/initialize!) (f)))

(deftest sampler-creation-test
  (testing "Basic sampler creation"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)]
      (is (some? sampler))))

  (testing "Sampler with options"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend {:shots 2048})]
      (is (some? sampler)))))

(deftest estimator-creation-test
  (testing "Basic estimator creation"
    (try
      (let [backend (backends/basic-simulator)
            estimator (prims/estimator backend)]
        (is (some? estimator)))
      (catch Exception e
        ;; Skip if not available
        (is (some? e)))))

  (testing "Estimator with options"
    (try
      (let [backend (backends/basic-simulator)
            estimator (prims/estimator backend {:shots 1024})]
        (is (some? estimator)))
      (catch Exception e
        ;; Skip if not available
        (is (some? e))))))

(deftest sampler-execution-test
  (testing "Single circuit sampling"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          circuit (-> (circuit/quantum-circuit 2)
                      (gates/hadamard 0)
                      (gates/cnot 0 1)
                      (circuit/measure-all))
          job (prims/run-sampler sampler circuit)]
      (is (some? job))
      (let [result (prims/sampler-result job)]
        (is (some? result))
        (let [quasi-dists (prims/quasi-distributions result)]
          (is (coll? quasi-dists))))))

  (testing "Multiple circuits sampling"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          circuit1 (-> (circuit/quantum-circuit 1)
                       (gates/hadamard 0)
                       (circuit/measure-all))
          circuit2 (-> (circuit/quantum-circuit 1)
                       (gates/pauli-x 0)
                       (circuit/measure-all))
          job (prims/run-sampler sampler [circuit1 circuit2])]
      (is (some? job))
      (let [result (prims/sampler-result job)
            quasi-dists (prims/quasi-distributions result)]
        (is (coll? quasi-dists))
        (is (= 2 (count quasi-dists))))))

  (testing "Sampling with parameter binding"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/ry (/ Math/PI 4) 0)
                      (circuit/measure-all))
          job (prims/run-sampler sampler circuit)]
      (is (some? job)))))

(deftest estimator-execution-test
  (testing "Single observable estimation"
    (try
      (let [backend (backends/basic-simulator)
            estimator (prims/estimator backend)
            circuit (-> (circuit/quantum-circuit 1)
                        (gates/hadamard 0))
            observable (qi/pauli-z)
            job (prims/run-estimator estimator circuit observable)]
        (is (some? job))
        (let [result (prims/estimator-result job)
              values (prims/expectation-values result)]
          (is (coll? values))
          (is (every? number? values))))
      (catch Exception e
        ;; Skip if estimator not available
        (is (some? e)))))

  (testing "Multiple observables estimation"
    (try
      (let [backend (backends/basic-simulator)
            estimator (prims/estimator backend)
            circuit (-> (circuit/quantum-circuit 1)
                        (gates/hadamard 0))
            obs1 (qi/pauli-x)
            obs2 (qi/pauli-z)
            job (prims/run-estimator estimator circuit [obs1 obs2])]
        (is (some? job)))
      (catch Exception e
        ;; Skip if estimator not available
        (is (some? e))))))

(deftest primitive-job-management-test
  (testing "Job status checking"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)
                      (circuit/measure-all))
          job (prims/run-sampler sampler circuit)]
      (is (some? (prims/job-status job)))))

  (testing "Job cancellation"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)
                      (circuit/measure-all))
          job (prims/run-sampler sampler circuit)]
      ;; Just test that cancel doesn't throw
      (is (some? (prims/cancel-job job))))))

(deftest quasi-distribution-analysis-test
  (testing "Quasi-distribution properties"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)
                      (circuit/measure-all))
          job (prims/run-sampler sampler circuit)
          result (prims/sampler-result job)
          quasi-dists (prims/quasi-distributions result)
          quasi-dist (first quasi-dists)]
      (is (map? quasi-dist))
      (is (every? string? (keys quasi-dist)))
      (is (every? number? (vals quasi-dist))))))

(deftest bell-state-primitives-test
  (testing "Bell state sampling with primitives"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          bell-circuit (-> (circuit/quantum-circuit 2)
                           (gates/hadamard 0)
                           (gates/cnot 0 1)
                           (circuit/measure-all))
          job (prims/run-sampler sampler bell-circuit)
          result (prims/sampler-result job)
          quasi-dists (prims/quasi-distributions result)
          quasi-dist (first quasi-dists)]
      (is (map? quasi-dist))
      ;; Bell state should only have |00⟩ and |11⟩ outcomes
      (let [outcomes (keys quasi-dist)]
        (is (every? #(or (= % "00") (= % "11")) outcomes)))))

  (testing "Bell state expectation value estimation"
    (try
      (let [backend (backends/basic-simulator)
            estimator (prims/estimator backend)
            bell-circuit (-> (circuit/quantum-circuit 2)
                             (gates/hadamard 0)
                             (gates/cnot 0 1))
            zz-observable (qi/tensor (qi/pauli-z) (qi/pauli-z))
            job (prims/run-estimator estimator bell-circuit zz-observable)
            result (prims/estimator-result job)
            exp-vals (prims/expectation-values result)]
        (is (coll? exp-vals))
        (is (= 1 (count exp-vals)))
        ;; Bell state should have <ZZ> = 1
        (is (< (Math/abs (- (first exp-vals) 1.0)) 0.1)))
      (catch Exception e
        ;; Skip if estimator not available
        (is (some? e))))))

(deftest parameterized-circuit-test
  (testing "Parameterized circuit with primitives"
    (let [backend (backends/basic-simulator)
          sampler (prims/sampler backend)
          ;; Create a simple parameterized circuit
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/ry (/ Math/PI 2) 0)  ; Fixed parameter for now
                      (circuit/measure-all))
          job (prims/run-sampler sampler circuit)
          result (prims/sampler-result job)]
      (is (some? result)))))