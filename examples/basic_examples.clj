(ns examples.basic-examples
  "Basic examples demonstrating qiskit-clj usage.

   These examples show fundamental quantum computing concepts
   implemented using the qiskit-clj library."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]
            [com.justinwoodring.qiskit-clj.backends :as backends]
            [com.justinwoodring.qiskit-clj.primitives :as primitives]))

;; Initialize the library
(core/initialize!)

(defn hello-quantum
  "Create and execute a simple quantum circuit."
  []
  (println "=== Hello Quantum ===")
  (let [;; Create a single qubit circuit
        qc (circuit/quantum-circuit 1)
        ;; Apply Hadamard gate to create superposition
        _ (gates/hadamard qc 0)
        ;; Add measurement
        _ (circuit/measure-all qc)]

    ;; Display the circuit
    (println "Circuit:")
    (println (circuit/draw qc))

    ;; Execute and get results
    (let [counts (backends/execute-circuit qc)]
      (println "Measurement results:")
      (println counts)
      counts)))

(defn bell-state-example
  "Create and analyze a Bell state (maximally entangled two-qubit state)."
  []
  (println "\\n=== Bell State ===")
  (let [;; Create Bell state circuit: |00⟩ + |11⟩
        bell-circuit (-> (circuit/quantum-circuit 2)
                         (gates/hadamard 0)      ; Put first qubit in superposition
                         (gates/cnot 0 1)        ; Entangle with second qubit
                         (circuit/measure-all))] ; Measure both qubits

    (println "Bell state circuit:")
    (println (circuit/draw bell-circuit))

    ;; Execute with different shot counts
    (let [counts-1000 (backends/execute-circuit bell-circuit nil 1000)
          counts-10000 (backends/execute-circuit bell-circuit nil 10000)]

      (println "Results with 1000 shots:")
      (println counts-1000)
      (println "Results with 10000 shots:")
      (println counts-10000)

      ;; Analyze the results
      (let [outcomes (keys counts-10000)
            total-shots (reduce + (vals counts-10000))]
        (println "\\nAnalysis:")
        (println "Possible outcomes:" outcomes)
        (println "Total shots:" total-shots)
        (doseq [outcome outcomes]
          (let [count (get counts-10000 outcome)
                probability (/ count total-shots)]
            (println (format "P(%s) = %.3f (%d/%d)" outcome probability count total-shots)))))

      counts-10000)))

(defn ghz-state-example
  "Create a 3-qubit GHZ state (generalization of Bell state)."
  []
  (println "\\n=== GHZ State ===")
  (let [;; Create GHZ state: |000⟩ + |111⟩
        ghz-circuit (-> (circuit/quantum-circuit 3)
                        (gates/hadamard 0)       ; Superposition on first qubit
                        (gates/cnot 0 1)         ; Entangle first and second
                        (gates/cnot 1 2)         ; Entangle second and third
                        (circuit/measure-all))]  ; Measure all qubits

    (println "GHZ state circuit:")
    (println (circuit/draw ghz-circuit))

    (let [counts (backends/execute-circuit ghz-circuit nil 5000)]
      (println "GHZ state measurement results:")
      (println counts)

      ;; Should only see |000⟩ and |111⟩ outcomes
      (let [expected-outcomes #{"000" "111"}
            actual-outcomes (set (keys counts))
            unexpected (clojure.set/difference actual-outcomes expected-outcomes)]
        (println "\\nExpected outcomes:" expected-outcomes)
        (println "Actual outcomes:" actual-outcomes)
        (when (seq unexpected)
          (println "Unexpected outcomes (likely due to noise):" unexpected)))

      counts)))

(defn superposition-example
  "Demonstrate quantum superposition with different bases."
  []
  (println "\\n=== Superposition in Different Bases ===")

  ;; Z-basis measurement
  (let [z-circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)  ; |+⟩ = (|0⟩ + |1⟩)/√2
                      (circuit/measure-all))]
    (println "Measuring |+⟩ state in Z-basis:")
    (let [z-counts (backends/execute-circuit z-circuit nil 1000)]
      (println z-counts)))

  ;; X-basis measurement (add Hadamard before measurement)
  (let [x-circuit (-> (circuit/quantum-circuit 1)
                      (gates/hadamard 0)   ; Create |+⟩
                      (gates/hadamard 0)   ; Rotate back to Z-basis for measurement
                      (circuit/measure-all))]
    (println "\\nMeasuring |+⟩ state in X-basis (Hadamard before measurement):")
    (let [x-counts (backends/execute-circuit x-circuit nil 1000)]
      (println x-counts))))

(defn rotation-example
  "Demonstrate quantum rotations and their effects."
  []
  (println "\\n=== Quantum Rotations ===")

  (let [angles [0 (/ Math/PI 4) (/ Math/PI 2) (* 3 (/ Math/PI 4)) Math/PI]]
    (println "RY rotations starting from |0⟩:")
    (doseq [angle angles]
      (let [circuit (-> (circuit/quantum-circuit 1)
                        (gates/rotation-y angle 0)
                        (circuit/measure-all))
            counts (backends/execute-circuit circuit nil 1000)
            prob-1 (/ (get counts "1" 0) 1000.0)]
        (println (format "RY(%.3fπ): P(|1⟩) = %.3f" (/ angle Math/PI) prob-1))))))

(defn phase-example
  "Demonstrate quantum phase and its measurement."
  []
  (println "\\n=== Quantum Phase ===")

  ;; Phase has no effect on Z-basis measurement
  (let [phase-circuit (-> (circuit/quantum-circuit 1)
                          (gates/hadamard 0)     ; Create |+⟩
                          (gates/s-gate 0)       ; Add phase
                          (gates/hadamard 0)     ; Back to Z-basis
                          (circuit/measure-all))]
    (println "Effect of S gate on |+⟩ state:")
    (let [counts (backends/execute-circuit phase-circuit nil 1000)]
      (println counts)))

  ;; Compare with no phase
  (let [no-phase-circuit (-> (circuit/quantum-circuit 1)
                             (gates/hadamard 0)   ; Create |+⟩
                             (gates/hadamard 0)   ; Back to Z-basis
                             (circuit/measure-all))]
    (println "\\nWithout S gate:")
    (let [counts (backends/execute-circuit no-phase-circuit nil 1000)]
      (println counts))))

(defn deutsch-algorithm
  "Implement Deutsch's algorithm for determining if a function is constant or balanced."
  [oracle-type]
  (println (format "\\n=== Deutsch Algorithm (Oracle: %s) ===" (name oracle-type)))

  (let [;; Create quantum circuit with ancilla qubit
        qc (circuit/quantum-circuit 2)

        ;; Initialize qubits: |0⟩|1⟩
        _ (gates/pauli-x qc 1)

        ;; Create superposition: |+⟩|-⟩
        _ (gates/hadamard qc [0 1])

        ;; Apply oracle based on type
        _ (case oracle-type
            :constant-0 nil  ; Do nothing (f(x) = 0)
            :constant-1 (gates/pauli-x qc 1)  ; Flip ancilla (f(x) = 1)
            :balanced-identity (gates/cnot qc 0 1)  ; f(x) = x
            :balanced-not (do (gates/pauli-x qc 0)   ; f(x) = NOT x
                              (gates/cnot qc 0 1)
                              (gates/pauli-x qc 0)))

        ;; Apply Hadamard to query qubit
        _ (gates/hadamard qc 0)

        ;; Measure only the query qubit
        _ (circuit/measure qc 0 0)]

    (println "Deutsch algorithm circuit:")
    (println (circuit/draw qc))

    (let [counts (backends/execute-circuit qc nil 1000)
          result-0 (get counts "0" 0)
          result-1 (get counts "1" 0)]
      (println "Measurement results:")
      (println counts)

      (let [prediction (if (> result-0 result-1) "constant" "balanced")]
        (println (format "Algorithm predicts function is: %s" prediction))
        (println (format "Actual function type: %s"
                         (if (#{:constant-0 :constant-1} oracle-type)
                           "constant"
                           "balanced")))
        prediction))))

(defn run-all-examples
  "Run all basic examples."
  []
  (println "Running qiskit-clj Basic Examples")
  (println "=================================\\n")

  ;; Run all examples
  (hello-quantum)
  (bell-state-example)
  (ghz-state-example)
  (superposition-example)
  (rotation-example)
  (phase-example)

  ;; Deutsch algorithm with different oracles
  (deutsch-algorithm :constant-0)
  (deutsch-algorithm :constant-1)
  (deutsch-algorithm :balanced-identity)
  (deutsch-algorithm :balanced-not)

  (println "\\n=== Examples Complete ==="))

(comment
  ;; Run individual examples:
  (hello-quantum)
  (bell-state-example)
  (ghz-state-example)
  (run-all-examples))

;; If this file is run directly, execute all examples
(when (= *file* (first *command-line-args*))
  (run-all-examples))