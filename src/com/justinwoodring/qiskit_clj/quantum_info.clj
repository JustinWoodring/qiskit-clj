(ns com.justinwoodring.qiskit-clj.quantum-info
  "Quantum information utilities for states, operators, and measurements.

   This namespace provides functions for working with quantum states, operators,
   and information-theoretic quantities like fidelity, entanglement measures,
   and distance metrics."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]))

;; Quantum state creation and manipulation
(defn statevector
  "Create a quantum state vector.

   Args:
   - data: Initial state data (vector of amplitudes or circuit)
   - validate: Whether to validate the state (default: true)

   Returns: Statevector object"
  ([data]
   (statevector data true))
  ([data validate]
   (core/with-qiskit
     (require-python '[qiskit.quantum_info :as 'qi])
     (py/call-attr 'qi "Statevector" (core/->py data) :validate validate))))

(defn zero-state
  "Create |0⟩^n state for n qubits."
  [n-qubits]
  (core/validate-qubit-count n-qubits)
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (py/call-attr 'qi "Statevector" :dims (py/->py-list (repeat n-qubits 2)))))

(defn plus-state
  "Create |+⟩^n state for n qubits."
  [n-qubits]
  (core/validate-qubit-count n-qubits)
  (let [amplitudes (repeat (Math/pow 2 n-qubits) (/ 1.0 (Math/sqrt (Math/pow 2 n-qubits))))]
    (statevector amplitudes)))

(defn bell-state
  "Create Bell state.

   Args:
   - bell-type: :phi-plus, :phi-minus, :psi-plus, or :psi-minus (default: :phi-plus)

   Returns: Statevector object"
  ([]
   (bell-state :phi-plus))
  ([bell-type]
   (let [sqrt2 (Math/sqrt 2)
         amplitudes (case bell-type
                      :phi-plus [1 0 0 1]
                      :phi-minus [1 0 0 -1]
                      :psi-plus [0 1 1 0]
                      :psi-minus [0 1 -1 0])]
     (statevector (map #(/ % sqrt2) amplitudes)))))

(defn ghz-state
  "Create GHZ state for n qubits."
  [n-qubits]
  (core/validate-qubit-count n-qubits)
  (let [dim (Math/pow 2 n-qubits)
        sqrt2 (Math/sqrt 2)
        amplitudes (assoc (vec (repeat dim 0))
                          0 (/ 1 sqrt2)
                          (dec dim) (/ 1 sqrt2))]
    (statevector amplitudes)))

(defn w-state
  "Create W state for n qubits."
  [n-qubits]
  (core/validate-qubit-count n-qubits)
  (let [dim (Math/pow 2 n-qubits)
        sqrt-n (Math/sqrt n-qubits)
        amplitudes (vec (repeat dim 0))
        ;; Set amplitudes for computational basis states with exactly one |1⟩
        single-one-states (for [i (range n-qubits)]
                            (Math/pow 2 i))]
    (statevector (reduce #(assoc %1 %2 (/ 1 sqrt-n))
                         amplitudes
                         single-one-states))))

;; State properties and measurements
(defn state-vector-data
  "Get the raw amplitude data from a statevector."
  [state]
  (core/->clj (py/get-attr state "data")))

(defn state-dims
  "Get the dimensions of a quantum state."
  [state]
  (core/->clj (py/get-attr state "dims")))

(defn num-qubits-from-state
  "Get the number of qubits from a state."
  [state]
  (py/call-attr state "num_qubits"))

(defn is-valid-state?
  "Check if a state is valid (normalized)."
  [state]
  (py/call-attr state "is_valid"))

(defn probability
  "Get probability of measuring a specific outcome.

   Args:
   - state: Statevector
   - outcome: Measurement outcome (binary string or integer)

   Returns: Probability"
  [state outcome]
  (let [outcome-int (if (string? outcome)
                      (Integer/parseInt outcome 2)
                      outcome)]
    (py/call-attr state "probabilities" [outcome-int])))

(defn probabilities
  "Get all measurement probabilities."
  [state]
  (core/->clj (py/call-attr state "probabilities")))

(defn sample-memory
  "Sample measurement outcomes from a state.

   Args:
   - state: Statevector
   - shots: Number of samples

   Returns: Vector of measurement outcomes (binary strings)"
  [state shots]
  (core/->clj (py/call-attr state "sample_memory" shots)))

(defn sample-counts
  "Sample measurement outcomes and return counts.

   Args:
   - state: Statevector
   - shots: Number of samples

   Returns: Map of outcomes to counts"
  [state shots]
  (core/->clj (py/call-attr state "sample_counts" shots)))

;; Density matrix operations
(defn density-matrix
  "Create a density matrix.

   Args:
   - data: Matrix data or Statevector

   Returns: DensityMatrix object"
  [data]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (py/call-attr 'qi "DensityMatrix" (core/->py data))))

(defn partial-trace
  "Compute partial trace of a density matrix.

   Args:
   - rho: DensityMatrix
   - qubits-to-trace: Qubits to trace out

   Returns: DensityMatrix of reduced system"
  [rho qubits-to-trace]
  (py/call-attr rho "partial_trace" (core/->py qubits-to-trace)))

(defn purity
  "Calculate purity of a density matrix.

   Returns: Purity value (1 for pure states, < 1 for mixed states)"
  [rho]
  (py/call-attr rho "purity"))

(defn von-neumann-entropy
  "Calculate von Neumann entropy of a density matrix.

   Args:
   - rho: DensityMatrix
   - base: Logarithm base (default: 2)

   Returns: Entropy value"
  ([rho]
   (von-neumann-entropy rho 2))
  ([rho base]
   (core/with-qiskit
     (require-python '[qiskit.quantum_info :as 'qi])
     (py/call-attr 'qi "entropy" rho :base base))))

;; Fidelity and distance measures
(defn state-fidelity
  "Calculate fidelity between two quantum states.

   Args:
   - state1: First quantum state
   - state2: Second quantum state

   Returns: Fidelity value (0 to 1)"
  [state1 state2]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (py/call-attr 'qi "state_fidelity" state1 state2)))

(defn process-fidelity
  "Calculate process fidelity between two quantum channels.

   Args:
   - channel1: First quantum channel
   - channel2: Second quantum channel

   Returns: Process fidelity value"
  [channel1 channel2]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (py/call-attr 'qi "process_fidelity" channel1 channel2)))

(defn trace-distance
  "Calculate trace distance between two states.

   Args:
   - state1: First quantum state
   - state2: Second quantum state

   Returns: Trace distance (0 to 1)"
  [state1 state2]
  (let [rho1 (if (= (type state1) (type (density-matrix [[1 0] [0 0]])))
               state1
               (density-matrix state1))
        rho2 (if (= (type state2) (type (density-matrix [[1 0] [0 0]])))
               state2
               (density-matrix state2))]
    (core/with-qiskit
      (require-python '[qiskit.quantum_info :as 'qi])
      (py/call-attr 'qi "trace_distance" rho1 rho2))))

(defn hellinger-fidelity
  "Calculate Hellinger fidelity between probability distributions.

   Args:
   - p: First probability distribution
   - q: Second probability distribution

   Returns: Hellinger fidelity"
  [p q]
  (let [p-vec (if (map? p) (vals p) p)
        q-vec (if (map? q) (vals q) q)]
    (reduce + (map #(Math/sqrt (* %1 %2)) p-vec q-vec))))

;; Entanglement measures
(defn entanglement-of-formation
  "Calculate entanglement of formation for a bipartite state.

   Args:
   - state: Bipartite quantum state
   - partition: Partition specification

   Returns: Entanglement of formation"
  [state partition]
  (let [rho (if (= (type state) (type (density-matrix [[1 0] [0 0]])))
              state
              (density-matrix state))
        reduced-rho (partial-trace rho partition)]
    (von-neumann-entropy reduced-rho)))

(defn concurrence
  "Calculate concurrence for a two-qubit state.

   Args:
   - state: Two-qubit quantum state

   Returns: Concurrence value (0 to 1)"
  [state]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (py/call-attr 'qi "concurrence" state)))

(defn entangling-power
  "Calculate entangling power of a quantum gate.

   Args:
   - gate: Quantum gate or unitary operator

   Returns: Entangling power"
  [gate]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (py/call-attr 'qi "entangling_power" gate)))

;; Pauli operators and observables
(defn pauli-operator
  "Create a Pauli operator.

   Args:
   - pauli-string: Pauli string (e.g., \"IXYZ\")
   - coefficient: Coefficient (default: 1)

   Returns: Pauli operator"
  ([pauli-string]
   (pauli-operator pauli-string 1))
  ([pauli-string coefficient]
   (core/with-qiskit
     (require-python '[qiskit.quantum_info :as 'qi])
     (py/call-attr 'qi "Pauli" pauli-string))))

(defn sparse-pauli-op
  "Create a sparse Pauli operator from multiple terms.

   Args:
   - pauli-terms: Collection of [pauli-string coefficient] pairs

   Returns: SparsePauliOp"
  [pauli-terms]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (let [pauli-strings (map first pauli-terms)
          coefficients (map second pauli-terms)]
      (py/call-attr 'qi "SparsePauliOp" pauli-strings :coeffs coefficients))))

(defn expectation-value
  "Calculate expectation value of an observable for a state.

   Args:
   - state: Quantum state
   - observable: Observable operator

   Returns: Expectation value"
  [state observable]
  (py/call-attr state "expectation_value" observable))

(defn variance
  "Calculate variance of an observable for a state.

   Args:
   - state: Quantum state
   - observable: Observable operator

   Returns: Variance"
  [state observable]
  (py/call-attr state "variance" observable))

;; Quantum channels and maps
(defn kraus-to-choi
  "Convert Kraus representation to Choi matrix.

   Args:
   - kraus-ops: Collection of Kraus operators

   Returns: Choi matrix"
  [kraus-ops]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (py/call-attr 'qi "kraus_to_choi" (core/->py kraus-ops))))

(defn choi-to-kraus
  "Convert Choi matrix to Kraus representation.

   Args:
   - choi-matrix: Choi matrix

   Returns: Kraus operators"
  [choi-matrix]
  (core/with-qiskit
    (require-python '[qiskit.quantum_info :as 'qi])
    (core/->clj (py/call-attr 'qi "choi_to_kraus" choi-matrix))))

;; Random quantum objects
(defn random-statevector
  "Generate a random quantum state.

   Args:
   - dims: Dimensions (number of qubits or dimension list)
   - seed: Random seed (optional)

   Returns: Random Statevector"
  ([dims]
   (random-statevector dims nil))
  ([dims seed]
   (core/with-qiskit
     (require-python '[qiskit.quantum_info.random :as 'random])
     (if seed
       (py/call-attr 'random "random_statevector" dims :seed seed)
       (py/call-attr 'random "random_statevector" dims)))))

(defn random-density-matrix
  "Generate a random density matrix.

   Args:
   - dims: Dimensions
   - rank: Rank of the matrix (optional)
   - seed: Random seed (optional)

   Returns: Random DensityMatrix"
  ([dims]
   (random-density-matrix dims nil nil))
  ([dims rank]
   (random-density-matrix dims rank nil))
  ([dims rank seed]
   (core/with-qiskit
     (require-python '[qiskit.quantum_info.random :as 'random])
     (let [kwargs (cond-> {}
                    rank (assoc :rank rank)
                    seed (assoc :seed seed))]
       (py/call-attr 'random "random_density_matrix" dims kwargs)))))

(defn random-unitary
  "Generate a random unitary matrix.

   Args:
   - dims: Dimensions
   - seed: Random seed (optional)

   Returns: Random Operator"
  ([dims]
   (random-unitary dims nil))
  ([dims seed]
   (core/with-qiskit
     (require-python '[qiskit.quantum_info.random :as 'random])
     (if seed
       (py/call-attr 'random "random_unitary" dims :seed seed)
       (py/call-attr 'random "random_unitary" dims)))))

(comment
  ;; Example usage
  (require '[com.justinwoodring.qiskit-clj.core :as core])
  (core/initialize!)

  ;; Create and analyze Bell state
  (let [bell (bell-state :phi-plus)
        probs (probabilities bell)
        entropy (von-neumann-entropy (density-matrix bell))]
    (println "Bell state probabilities:" probs)
    (println "Bell state entropy:" entropy))

  ;; Fidelity between states
  (let [state1 (zero-state 2)
        state2 (plus-state 2)
        fid (state-fidelity state1 state2)]
    (println "Fidelity between |00⟩ and |++⟩:" fid))

  ;; Entanglement analysis
  (let [bell (bell-state)
        dm (density-matrix bell)
        reduced (partial-trace dm [1])
        ent (von-neumann-entropy reduced)]
    (println "Entanglement entropy of Bell state:" ent)))