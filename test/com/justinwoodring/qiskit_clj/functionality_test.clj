(ns com.justinwoodring.qiskit-clj.functionality-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]))

(deftest core-functionality-test
  (testing "Core system works end-to-end"
    (core/initialize!)

    ;; Test circuit creation
    (let [qc (circuit/quantum-circuit 2)]
      (is (= 2 (circuit/num-qubits qc)))

      ;; Test gate operations with our safety improvements
      (let [bell-circuit (-> qc
                            (gates/hadamard 0)
                            (gates/cnot 0 1))]
        (is (= 2 (circuit/circuit-size bell-circuit)))

        ;; Test safe circuit printing
        (let [description (circuit/print-circuit bell-circuit)]
          (is (string? description))
          (is (> (count description) 0)))))))