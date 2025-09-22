(ns com.justinwoodring.qiskit-clj.primitives
  "Quantum primitives for circuit execution and measurement.

   This namespace provides idiomatic Clojure wrappers for Qiskit's primitive
   functions: Sampler for quantum state sampling and Estimator for observable
   expectation value calculations."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]))

;; Sampler primitive for quantum state sampling
(defn create-sampler
  "Create a Sampler primitive for quantum circuit sampling.

   Options:
   - :backend - Backend to use (default: local simulator)
   - :options - Backend-specific options

   Returns: Sampler object"
  ([]
   (create-sampler {}))
  ([{:keys [backend options]}]
   (core/with-qiskit
     (let [qprimitives (py/import-module "qiskit.primitives")]
       (if backend
         (py/call-attr qprimitives "Sampler" :backend backend :options options)
         (py/call-attr qprimitives "Sampler"))))))

(defn run-sampler
  "Run circuits using the Sampler primitive.

   Args:
   - sampler: Sampler object
   - circuits: Single circuit or collection of circuits
   - parameter-values: Parameter values for parameterized circuits (optional)
   - shots: Number of shots (optional)

   Returns: SamplerResult object"
  ([sampler circuits]
   (run-sampler sampler circuits nil nil))
  ([sampler circuits parameter-values]
   (run-sampler sampler circuits parameter-values nil))
  ([sampler circuits parameter-values shots]
   (let [circuits-list (if (coll? circuits) circuits [circuits])
         kwargs (cond-> {}
                  parameter-values (assoc :parameter_values (core/->py parameter-values))
                  shots (assoc :shots shots))]
     (core/->clj (py/call-attr sampler "run" circuits-list kwargs)))))

(defn sampler-result->counts
  "Extract measurement counts from SamplerResult.

   Args:
   - result: SamplerResult object
   - circuit-index: Index of circuit result (default: 0)

   Returns: Map of measurement outcomes to counts"
  ([result]
   (sampler-result->counts result 0))
  ([result circuit-index]
   (let [quasi-dists (py/get-attr result "quasi_dists")]
     (core/->clj (py/get-item quasi-dists circuit-index)))))

(defn sampler-result->probabilities
  "Extract probabilities from SamplerResult."
  ([result]
   (sampler-result->probabilities result 0))
  ([result circuit-index]
   (let [counts (sampler-result->counts result circuit-index)
         total (reduce + (vals counts))]
     (into {} (map (fn [[outcome count]]
                     [outcome (/ count total)])
                   counts)))))

;; Estimator primitive for expectation value calculations
(defn create-estimator
  "Create an Estimator primitive for observable expectation values.

   Options:
   - :backend - Backend to use (default: local simulator)
   - :options - Backend-specific options

   Returns: Estimator object"
  ([]
   (create-estimator {}))
  ([{:keys [backend options]}]
   (core/with-qiskit
     (let [qprimitives (py/import-module "qiskit.primitives")]
       (if backend
         (py/call-attr qprimitives "Estimator" :backend backend :options options)
         (py/call-attr qprimitives "Estimator"))))))

(defn run-estimator
  "Run circuits with observables using the Estimator primitive.

   Args:
   - estimator: Estimator object
   - circuits: Single circuit or collection of circuits
   - observables: Single observable or collection of observables
   - parameter-values: Parameter values for parameterized circuits (optional)
   - shots: Number of shots (optional)

   Returns: EstimatorResult object"
  ([estimator circuits observables]
   (run-estimator estimator circuits observables nil nil))
  ([estimator circuits observables parameter-values]
   (run-estimator estimator circuits observables parameter-values nil))
  ([estimator circuits observables parameter-values shots]
   (let [circuits-list (if (coll? circuits) circuits [circuits])
         observables-list (if (coll? observables) observables [observables])
         kwargs (cond-> {}
                  parameter-values (assoc :parameter_values (core/->py parameter-values))
                  shots (assoc :shots shots))]
     (core/->clj (py/call-attr estimator "run" circuits-list observables-list kwargs)))))

(defn estimator-result->values
  "Extract expectation values from EstimatorResult.

   Args:
   - result: EstimatorResult object

   Returns: Vector of expectation values"
  [result]
  (core/->clj (py/get-attr result "values")))

(defn estimator-result->variances
  "Extract variances from EstimatorResult.

   Args:
   - result: EstimatorResult object

   Returns: Vector of variances (if available)"
  [result]
  (try
    (core/->clj (py/get-attr result "variances"))
    (catch Exception _ nil)))

;; Observable construction helpers
(defn pauli-observable
  "Create a Pauli observable from a string.

   Args:
   - pauli-string: Pauli string (e.g., \"IXYZ\", \"ZZ\")
   - coefficient: Coefficient (default: 1.0)

   Returns: SparsePauliOp object"
  ([pauli-string]
   (pauli-observable pauli-string 1.0))
  ([pauli-string coefficient]
   (core/with-qiskit
     (let [qi (py/import-module "qiskit.quantum_info")]
       (py/call-attr qi "SparsePauliOp" pauli-string :coeffs [coefficient])))))

(defn pauli-sum
  "Create a sum of Pauli observables.

   Args:
   - pauli-terms: Collection of [pauli-string coefficient] pairs

   Returns: SparsePauliOp object"
  [pauli-terms]
  (core/with-qiskit
    (let [qi (py/import-module "qiskit.quantum_info")
          pauli-strings (map first pauli-terms)
          coefficients (map second pauli-terms)]
      (py/call-attr qi "SparsePauliOp" pauli-strings :coeffs coefficients))))

(defn z-observable
  "Create Z observable for specified qubits.

   Args:
   - n-qubits: Total number of qubits
   - target-qubits: Qubits to measure (single index or collection)

   Returns: SparsePauliOp object"
  [n-qubits target-qubits]
  (let [qubit-list (if (coll? target-qubits) target-qubits [target-qubits])
        pauli-string (apply str (for [i (range n-qubits)]
                                  (if (some #(= i %) qubit-list) "Z" "I")))]
    (pauli-observable pauli-string)))

(defn x-observable
  "Create X observable for specified qubits."
  [n-qubits target-qubits]
  (let [qubit-list (if (coll? target-qubits) target-qubits [target-qubits])
        pauli-string (apply str (for [i (range n-qubits)]
                                  (if (some #(= i %) qubit-list) "X" "I")))]
    (pauli-observable pauli-string)))

(defn y-observable
  "Create Y observable for specified qubits."
  [n-qubits target-qubits]
  (let [qubit-list (if (coll? target-qubits) target-qubits [target-qubits])
        pauli-string (apply str (for [i (range n-qubits)]
                                  (if (some #(= i %) qubit-list) "Y" "I")))]
    (pauli-observable pauli-string)))

;; Higher-level execution functions
(defn sample-circuit
  "High-level function to sample a quantum circuit.

   Args:
   - circuit: QuantumCircuit to sample
   - shots: Number of shots (default: 1024)
   - backend: Backend to use (optional)

   Returns: Map of measurement outcomes to counts"
  ([circuit]
   (sample-circuit circuit 1024 nil))
  ([circuit shots]
   (sample-circuit circuit shots nil))
  ([circuit shots backend]
   (let [sampler (create-sampler (when backend {:backend backend}))
         result (run-sampler sampler circuit nil shots)]
     (sampler-result->counts result))))

(defn measure-expectation
  "High-level function to measure expectation value.

   Args:
   - circuit: QuantumCircuit to measure
   - observable: Observable to measure
   - shots: Number of shots (default: 1024)
   - backend: Backend to use (optional)

   Returns: Expectation value"
  ([circuit observable]
   (measure-expectation circuit observable 1024 nil))
  ([circuit observable shots]
   (measure-expectation circuit observable shots nil))
  ([circuit observable shots backend]
   (let [estimator (create-estimator (when backend {:backend backend}))
         result (run-estimator estimator circuit observable nil shots)
         values (estimator-result->values result)]
     (first values))))

(defn measure-pauli-expectation
  "Measure expectation value of a Pauli observable.

   Args:
   - circuit: QuantumCircuit
   - pauli-string: Pauli string (e.g., \"ZZ\", \"XIX\")
   - shots: Number of shots (default: 1024)

   Returns: Expectation value"
  ([circuit pauli-string]
   (measure-pauli-expectation circuit pauli-string 1024))
  ([circuit pauli-string shots]
   (let [observable (pauli-observable pauli-string)]
     (measure-expectation circuit observable shots))))

;; Batch execution utilities
(defn batch-sample
  "Sample multiple circuits in batch.

   Args:
   - circuits: Collection of circuits
   - shots: Number of shots per circuit
   - backend: Backend to use (optional)

   Returns: Vector of count maps"
  ([circuits shots]
   (batch-sample circuits shots nil))
  ([circuits shots backend]
   (let [sampler (create-sampler (when backend {:backend backend}))
         result (run-sampler sampler circuits nil shots)]
     (mapv #(sampler-result->counts result %) (range (count circuits))))))

(defn batch-measure
  "Measure multiple circuit-observable pairs in batch.

   Args:
   - circuit-observable-pairs: Collection of [circuit observable] pairs
   - shots: Number of shots per measurement
   - backend: Backend to use (optional)

   Returns: Vector of expectation values"
  ([circuit-observable-pairs shots]
   (batch-measure circuit-observable-pairs shots nil))
  ([circuit-observable-pairs shots backend]
   (let [estimator (create-estimator (when backend {:backend backend}))
         circuits (map first circuit-observable-pairs)
         observables (map second circuit-observable-pairs)
         result (run-estimator estimator circuits observables nil shots)]
     (estimator-result->values result))))

;; Result analysis utilities
(defn most-frequent-outcome
  "Find the most frequently measured outcome.

   Args:
   - counts: Map of outcomes to counts

   Returns: Most frequent outcome string"
  [counts]
  (key (apply max-key val counts)))

(defn outcome-probability
  "Calculate probability of a specific outcome.

   Args:
   - counts: Map of outcomes to counts
   - outcome: Outcome string

   Returns: Probability (0.0 to 1.0)"
  [counts outcome]
  (let [total (reduce + (vals counts))]
    (/ (get counts outcome 0) total)))

(defn fidelity-from-counts
  "Estimate state fidelity from measurement counts.

   Args:
   - counts1: First set of counts
   - counts2: Second set of counts

   Returns: Estimated fidelity"
  [counts1 counts2]
  (let [total1 (reduce + (vals counts1))
        total2 (reduce + (vals counts2))
        all-outcomes (set (concat (keys counts1) (keys counts2)))]
    (reduce + (for [outcome all-outcomes]
                (let [p1 (/ (get counts1 outcome 0) total1)
                      p2 (/ (get counts2 outcome 0) total2)]
                  (* (Math/sqrt p1) (Math/sqrt p2)))))))

(comment
  ;; Example usage
  (require '[com.justinwoodring.qiskit-clj.core :as core])
  (require '[com.justinwoodring.qiskit-clj.circuit :as circuit])
  (require '[com.justinwoodring.qiskit-clj.gates :as gates])
  (core/initialize!)

  ;; Sampling example
  (let [bell-circuit (-> (circuit/quantum-circuit 2)
                         (gates/hadamard 0)
                         (gates/cnot 0 1)
                         (circuit/measure-all))
        counts (sample-circuit bell-circuit 1000)]
    (println "Bell state counts:" counts))

  ;; Expectation value example
  (let [circuit (-> (circuit/quantum-circuit 1)
                    (gates/hadamard 0))
        z-obs (z-observable 1 0)
        expectation (measure-expectation circuit z-obs 1000)]
    (println "⟨Z⟩ for |+⟩ state:" expectation))

  ;; Pauli observable example
  (let [circuit (-> (circuit/quantum-circuit 2)
                    (gates/hadamard 0)
                    (gates/cnot 0 1))
        zz-expectation (measure-pauli-expectation circuit "ZZ" 1000)]
    (println "⟨ZZ⟩ for Bell state:" zz-expectation)))