(ns com.justinwoodring.qiskit-clj.quantum-info-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]
            [com.justinwoodring.qiskit-clj.quantum-info :as qi]))

(use-fixtures :once (fn [f] (core/initialize!) (f)))

(deftest statevector-creation-test
  (testing "Statevector from data"
    (let [data [1 0 0 0]
          sv (qi/statevector data)]
      (is (some? sv))
      (is (= 4 (qi/statevector-dim sv)))))

  (testing "Zero statevector"
    (let [sv (qi/zero-state 2)]
      (is (some? sv))
      (is (= 4 (qi/statevector-dim sv)))))

  (testing "One statevector"
    (let [sv (qi/one-state 2)]
      (is (some? sv))
      (is (= 4 (qi/statevector-dim sv)))))

  (testing "Plus statevector"
    (let [sv (qi/plus-state 1)]
      (is (some? sv))
      (is (= 2 (qi/statevector-dim sv)))))

  (testing "Minus statevector"
    (let [sv (qi/minus-state 1)]
      (is (some? sv))
      (is (= 2 (qi/statevector-dim sv))))))

(deftest statevector-properties-test
  (testing "Statevector dimension"
    (let [sv (qi/zero-state 3)]
      (is (= 8 (qi/statevector-dim sv)))))

  (testing "Statevector data access"
    (let [sv (qi/zero-state 1)
          data (qi/statevector-data sv)]
      (is (coll? data))
      (is (= 2 (count data)))))

  (testing "Statevector is valid"
    (let [sv (qi/zero-state 2)]
      (is (true? (qi/is-statevector? sv)))))

  (testing "Statevector probabilities"
    (let [sv (qi/plus-state 1)
          probs (qi/probabilities sv)]
      (is (coll? probs))
      (is (= 2 (count probs)))
      (is (every? #(and (>= % 0) (<= % 1)) probs))))

  (testing "Statevector probability for specific outcome"
    (let [sv (qi/zero-state 1)]
      (is (= 1.0 (qi/probability sv "0")))
      (is (= 0.0 (qi/probability sv "1"))))))

(deftest statevector-operations-test
  (testing "Statevector conjugate"
    (let [sv (qi/plus-state 1)
          conj-sv (qi/conjugate sv)]
      (is (some? conj-sv))
      (is (= (qi/statevector-dim sv) (qi/statevector-dim conj-sv)))))

  (testing "Statevector tensor product"
    (let [sv1 (qi/zero-state 1)
          sv2 (qi/one-state 1)
          tensor-sv (qi/tensor sv1 sv2)]
      (is (some? tensor-sv))
      (is (= 4 (qi/statevector-dim tensor-sv)))))

  (testing "Statevector evolve"
    (let [sv (qi/zero-state 1)
          circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0))
          evolved-sv (qi/evolve sv circuit)]
      (is (some? evolved-sv))
      (is (= 2 (qi/statevector-dim evolved-sv))))))

(deftest density-matrix-test
  (testing "Density matrix from statevector"
    (let [sv (qi/zero-state 2)
          dm (qi/density-matrix sv)]
      (is (some? dm))
      (is (= 4 (qi/density-matrix-dim dm)))))

  (testing "Density matrix properties"
    (let [sv (qi/plus-state 1)
          dm (qi/density-matrix sv)]
      (is (true? (qi/density-matrix? dm)))
      (is (= 2 (qi/density-matrix-dim dm)))))

  (testing "Density matrix trace"
    (let [sv (qi/zero-state 1)
          dm (qi/density-matrix sv)
          trace (qi/trace dm)]
      (is (number? trace))
      (is (< (Math/abs (- trace 1.0)) 1e-10))))

  (testing "Density matrix purity"
    (let [sv (qi/zero-state 1)
          dm (qi/density-matrix sv)
          purity (qi/purity dm)]
      (is (number? purity))
      (is (< (Math/abs (- purity 1.0)) 1e-10)))))

(deftest pauli-operators-test
  (testing "Pauli I operator"
    (let [pauli-i (qi/pauli-i)]
      (is (some? pauli-i))))

  (testing "Pauli X operator"
    (let [pauli-x (qi/pauli-x)]
      (is (some? pauli-x))))

  (testing "Pauli Y operator"
    (let [pauli-y (qi/pauli-y)]
      (is (some? pauli-y))))

  (testing "Pauli Z operator"
    (let [pauli-z (qi/pauli-z)]
      (is (some? pauli-z))))

  (testing "Pauli operator from string"
    (let [pauli-xx (qi/pauli "XX")]
      (is (some? pauli-xx))))

  (testing "SparsePauliOp creation"
    (let [sparse-pauli (qi/sparse-pauli-op [["XYZ" 0.5]])]
      (is (some? sparse-pauli)))))

(deftest operator-properties-test
  (testing "Operator dimension"
    (let [pauli-x (qi/pauli-x)]
      (is (= 2 (qi/operator-dim pauli-x)))))

  (testing "Operator is unitary"
    (let [pauli-x (qi/pauli-x)]
      (is (true? (qi/unitary? pauli-x)))))

  (testing "Operator is Hermitian"
    (let [pauli-x (qi/pauli-x)]
      (is (true? (qi/hermitian? pauli-x))))))

(deftest operator-operations-test
  (testing "Operator compose"
    (let [pauli-x (qi/pauli-x)
          pauli-y (qi/pauli-y)
          composed (qi/compose pauli-x pauli-y)]
      (is (some? composed))))

  (testing "Operator tensor"
    (let [pauli-x (qi/pauli-x)
          pauli-z (qi/pauli-z)
          tensor-op (qi/tensor pauli-x pauli-z)]
      (is (some? tensor-op))
      (is (= 4 (qi/operator-dim tensor-op)))))

  (testing "Operator power"
    (let [pauli-x (qi/pauli-x)
          power-op (qi/power pauli-x 2)]
      (is (some? power-op))))

  (testing "Operator adjoint"
    (let [pauli-x (qi/pauli-x)
          adj-op (qi/adjoint pauli-x)]
      (is (some? adj-op)))))

(deftest measurement-test
  (testing "Expectation value"
    (let [sv (qi/zero-state 1)
          pauli-z (qi/pauli-z)
          exp-val (qi/expectation-value sv pauli-z)]
      (is (number? exp-val))
      (is (< (Math/abs (- exp-val 1.0)) 1e-10))))

  (testing "Variance"
    (let [sv (qi/zero-state 1)
          pauli-z (qi/pauli-z)
          var (qi/variance sv pauli-z)]
      (is (number? var))
      (is (< (Math/abs var) 1e-10)))))

(deftest bell-state-quantum-info-test
  (testing "Bell state analysis"
    (let [circuit (-> (circuit/quantum-circuit 2)
                      (gates/hadamard 0)
                      (gates/cnot 0 1))
          sv (qi/statevector-from-circuit circuit)]
      (is (some? sv))
      (is (= 4 (qi/statevector-dim sv)))
      (let [probs (qi/probabilities sv)]
        (is (= 4 (count probs)))
        ;; Bell state should have equal probability for |00⟩ and |11⟩
        (is (< (Math/abs (- (nth probs 0) 0.5)) 1e-10))
        (is (< (Math/abs (- (nth probs 3) 0.5)) 1e-10))
        (is (< (Math/abs (nth probs 1)) 1e-10))
        (is (< (Math/abs (nth probs 2)) 1e-10))))))