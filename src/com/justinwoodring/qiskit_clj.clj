(ns com.justinwoodring.qiskit-clj
  "Main namespace for qiskit-clj providing convenient access to all functionality.

   This namespace re-exports the most commonly used functions from all modules,
   providing a single import point for basic quantum computing operations."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]
            [com.justinwoodring.qiskit-clj.primitives :as primitives]
            [com.justinwoodring.qiskit-clj.backends :as backends]
            [com.justinwoodring.qiskit-clj.quantum-info :as qi]))

;; Re-export core initialization functions
(def initialize! core/initialize!)
(def ensure-initialized! core/ensure-initialized!)
(def qiskit-version core/qiskit-version)

;; Re-export circuit creation and basic operations
(def quantum-circuit circuit/quantum-circuit)
(def num-qubits circuit/num-qubits)
(def num-clbits circuit/num-clbits)
(def circuit-depth circuit/circuit-depth)
(def circuit-size circuit/circuit-size)
(def measure circuit/measure)
(def measure-all circuit/measure-all)
(def barrier circuit/barrier)
(def draw circuit/draw)

;; Re-export common gates
(def h gates/hadamard)
(def x gates/pauli-x)
(def y gates/pauli-y)
(def z gates/pauli-z)
(def s gates/s-gate)
(def t gates/t-gate)
(def cx gates/cnot)
(def cz gates/controlled-z)
(def rx gates/rotation-x)
(def ry gates/rotation-y)
(def rz gates/rotation-z)
(def swap gates/swap-gate)
(def toffoli gates/toffoli)

;; Re-export execution functions
(def execute backends/execute-circuit)
(def sample primitives/sample-circuit)
(def measure-expectation primitives/measure-expectation)

;; Re-export backend creation
(def aer-simulator backends/aer-simulator)
(def basic-simulator backends/basic-simulator)

;; Re-export common quantum states
(def bell-state qi/bell-state)
(def ghz-state qi/ghz-state)
(def zero-state qi/zero-state)
(def plus-state qi/plus-state)

;; Re-export observables
(def pauli-observable primitives/pauli-observable)
(def z-observable primitives/z-observable)
(def x-observable primitives/x-observable)

;; Convenience macros and functions
(defmacro with-qiskit
  "Execute body with Qiskit environment initialized."
  [& body]
  `(do
     (ensure-initialized!)
     ~@body))

(defn hello-quantum
  "Create and execute a simple 'Hello Quantum' circuit.

   This creates a single qubit in superposition and measures it,
   demonstrating the basic quantum effect of randomness.

   Returns: Map of measurement outcomes to counts"
  ([]
   (hello-quantum 1000))
  ([shots]
   (with-qiskit
     (let [qc (-> (quantum-circuit 1)
                  (h 0)
                  (measure-all))]
       (execute qc nil shots)))))

(defn bell-pair
  "Create and execute a Bell state circuit.

   This creates a maximally entangled two-qubit state where measuring
   one qubit instantly determines the other.

   Returns: Map of measurement outcomes to counts"
  ([]
   (bell-pair 1000))
  ([shots]
   (with-qiskit
     (let [qc (-> (quantum-circuit 2)
                  (h 0)
                  (cx 0 1)
                  (measure-all))]
       (execute qc nil shots)))))

(defn ghz-triple
  "Create and execute a 3-qubit GHZ state circuit.

   This creates a 3-qubit entangled state similar to Bell states
   but demonstrating genuine multipartite entanglement.

   Returns: Map of measurement outcomes to counts"
  ([]
   (ghz-triple 1000))
  ([shots]
   (with-qiskit
     (let [qc (-> (quantum-circuit 3)
                  (h 0)
                  (cx 0 1)
                  (cx 1 2)
                  (measure-all))]
       (execute qc nil shots)))))

(defn random-circuit
  "Generate a random quantum circuit for testing.

   Args:
   - n-qubits: Number of qubits
   - depth: Circuit depth (number of gate layers)
   - gate-set: Set of gates to use (default: [:h :x :y :z :cx])

   Returns: Random QuantumCircuit"
  ([n-qubits depth]
   (random-circuit n-qubits depth [:h :x :y :z :cx]))
  ([n-qubits depth gate-set]
   (with-qiskit
     (let [qc (quantum-circuit n-qubits)
           available-gates (set gate-set)]
       (dotimes [_ depth]
         (let [gate (rand-nth (vec available-gates))]
           (case gate
             :h (h qc (rand-int n-qubits))
             :x (x qc (rand-int n-qubits))
             :y (y qc (rand-int n-qubits))
             :z (z qc (rand-int n-qubits))
             :s (s qc (rand-int n-qubits))
             :t (t qc (rand-int n-qubits))
             :cx (let [c (rand-int n-qubits)
                       t (rand-int n-qubits)]
                   (when (not= c t)
                     (cx qc c t)))
             :cz (let [c (rand-int n-qubits)
                       t (rand-int n-qubits)]
                   (when (not= c t)
                     (cz qc c t))))))
       (measure-all qc)))))

(defn benchmark-circuit
  "Benchmark circuit execution time.

   Args:
   - circuit: Circuit to benchmark
   - shots: Number of shots
   - iterations: Number of timing iterations (default: 5)

   Returns: Map with timing statistics"
  ([circuit shots]
   (benchmark-circuit circuit shots 5))
  ([circuit shots iterations]
   (with-qiskit
     (let [times (for [_ (range iterations)]
                   (let [start (System/nanoTime)
                         _ (execute circuit nil shots)
                         end (System/nanoTime)]
                     (/ (- end start) 1e6)))  ; Convert to milliseconds
           avg-time (/ (reduce + times) (count times))
           min-time (apply min times)
           max-time (apply max times)]
       {:average-ms avg-time
        :min-ms min-time
        :max-ms max-time
        :shots shots
        :iterations iterations
        :times times}))))

(defn circuit-info
  "Get comprehensive information about a quantum circuit.

   Args:
   - circuit: QuantumCircuit to analyze

   Returns: Map with circuit properties"
  [circuit]
  {:num-qubits (num-qubits circuit)
   :num-clbits (num-clbits circuit)
   :depth (circuit-depth circuit)
   :size (circuit-size circuit)
   :width (circuit/circuit-width circuit)})

(defn verify-installation
  "Verify that qiskit-clj is properly installed and working.

   This function tests basic functionality and reports the status.

   Returns: Map with verification results"
  []
  (try
    (initialize!)
    (let [version (qiskit-version)
          simple-circuit (-> (quantum-circuit 1)
                             (h 0)
                             (measure-all))
          results (execute simple-circuit nil 100)
          backend-available? (try
                               (aer-simulator)
                               true
                               (catch Exception _ false))]
      {:status :success
       :qiskit-version version
       :python-initialized true
       :simple-circuit-works (> (count results) 0)
       :aer-available backend-available?
       :message "qiskit-clj is working correctly!"})
    (catch Exception e
      {:status :error
       :error (str e)
       :message "qiskit-clj installation has issues."})))

(comment
  ;; Quick start examples
  (initialize!)

  ;; Hello quantum
  (hello-quantum)

  ;; Bell state
  (bell-pair)

  ;; GHZ state
  (ghz-triple)

  ;; Create custom circuit
  (-> (quantum-circuit 2)
      (h 0)
      (cx 0 1)
      (draw))

  ;; Verify installation
  (verify-installation)

  ;; Benchmark
  (let [qc (-> (quantum-circuit 3)
               (h 0)
               (cx 0 1)
               (cx 1 2)
               (measure-all))]
    (benchmark-circuit qc 1000)))

;; Library metadata
(def ^:const version "0.1.0-SNAPSHOT")
(def ^:const description "Idiomatic Clojure facade for Qiskit quantum computing")
(def ^:const author "Justin Woodring")
(def ^:const url "https://github.com/justinwoodring/qiskit-clj")