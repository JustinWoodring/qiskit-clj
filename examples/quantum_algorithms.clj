(ns examples.quantum-algorithms
  "Advanced quantum algorithm examples using qiskit-clj.

   This file demonstrates implementations of well-known quantum algorithms
   including Grover's search, Quantum Fourier Transform, and variational algorithms."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [com.justinwoodring.qiskit-clj.circuit :as circuit]
            [com.justinwoodring.qiskit-clj.gates :as gates]
            [com.justinwoodring.qiskit-clj.backends :as backends]
            [com.justinwoodring.qiskit-clj.primitives :as primitives]
            [com.justinwoodring.qiskit-clj.quantum-info :as qi]))

;; Initialize the library
(core/initialize!)

(defn diffusion-operator
  "Apply the diffusion operator (amplitude amplification about average) for Grover's algorithm."
  [circuit n-qubits]
  ;; H gates
  (gates/hadamard circuit (range n-qubits))
  ;; X gates
  (gates/pauli-x circuit (range n-qubits))
  ;; Multi-controlled Z gate (phase flip about |11...1⟩)
  (if (= n-qubits 1)
    (gates/pauli-z circuit 0)
    (gates/multi-controlled-z circuit (range (dec n-qubits)) (dec n-qubits)))
  ;; X gates
  (gates/pauli-x circuit (range n-qubits))
  ;; H gates
  (gates/hadamard circuit (range n-qubits))
  circuit)

(defn oracle-2bit-marked-11
  "Oracle that marks the |11⟩ state for 2-qubit Grover search."
  [circuit]
  (gates/controlled-z circuit 0 1))

(defn grover-2qubit
  "Implement Grover's algorithm for 2 qubits, searching for |11⟩ state."
  []
  (println "\\n=== Grover's Algorithm (2-qubit, search for |11⟩) ===")

  (let [n-qubits 2
        ;; For 2 qubits, optimal iterations ≈ π/4 * √(2^n) ≈ π/4 * 2 ≈ 1.57 ≈ 1
        iterations 1

        qc (circuit/quantum-circuit n-qubits)]

    ;; Initialize in equal superposition
    (gates/hadamard qc (range n-qubits))

    ;; Grover iterations
    (dotimes [_ iterations]
      ;; Oracle: mark |11⟩
      (oracle-2bit-marked-11 qc)
      ;; Diffusion operator
      (diffusion-operator qc n-qubits))

    ;; Measurement
    (circuit/measure-all qc)

    (println "Grover circuit:")
    (println (circuit/draw qc))

    (let [counts (backends/execute-circuit qc nil 5000)]
      (println "Results after Grover's algorithm:")
      (println counts)

      ;; Calculate success probability
      (let [target-count (get counts "11" 0)
            total-shots (reduce + (vals counts))
            success-prob (/ target-count total-shots)]
        (println (format "Success probability: %.3f (found |11⟩ %d/%d times)"
                         success-prob target-count total-shots))

        ;; Compare with random search
        (let [random-prob 0.25]  ; 1/4 for 2 qubits
          (println (format "Random search probability: %.3f" random-prob))
          (println (format "Grover's advantage: %.1fx improvement"
                           (/ success-prob random-prob)))))

      counts)))

(defn qft-rotations
  "Apply the rotation gates for QFT on a range of qubits."
  [circuit qubits]
  (let [n (count qubits)]
    (doseq [i (range n)]
      (let [qubit (nth qubits i)]
        ;; Hadamard gate
        (gates/hadamard circuit qubit)
        ;; Controlled rotation gates
        (doseq [j (range (inc i) n)]
          (let [control-qubit (nth qubits j)
                k (- j i)
                angle (/ Math/PI (Math/pow 2 k))]
            (gates/controlled-phase circuit angle control-qubit qubit)))))))

(defn swap-qubits
  "Swap the order of qubits (bit reversal for QFT)."
  [circuit qubits]
  (let [n (count qubits)]
    (doseq [i (range (quot n 2))]
      (let [qubit1 (nth qubits i)
            qubit2 (nth qubits (- n 1 i))]
        (gates/swap-gate circuit qubit1 qubit2)))))

(defn quantum-fourier-transform
  "Apply Quantum Fourier Transform to specified qubits."
  [circuit qubits]
  (qft-rotations circuit qubits)
  (swap-qubits circuit qubits)
  circuit)

(defn qft-example
  "Demonstrate Quantum Fourier Transform on a 3-qubit state."
  []
  (println "\\n=== Quantum Fourier Transform (3-qubit) ===")

  (let [n-qubits 3
        qc (circuit/quantum-circuit n-qubits)]

    ;; Prepare an interesting input state |101⟩
    (gates/pauli-x qc 0)  ; Set qubit 0 to |1⟩
    (gates/pauli-x qc 2)  ; Set qubit 2 to |1⟩
    ;; Qubit 1 remains |0⟩, so we have |101⟩

    (println "Input state preparation (|101⟩):")
    (let [prep-circuit (circuit/copy-circuit qc)]
      (circuit/measure-all prep-circuit)
      (println (circuit/draw prep-circuit)))

    ;; Apply QFT
    (quantum-fourier-transform qc (range n-qubits))

    ;; Measure the result
    (circuit/measure-all qc)

    (println "\\nQFT circuit:")
    (println (circuit/draw qc))

    (let [counts (backends/execute-circuit qc nil 5000)]
      (println "QFT output distribution:")
      (println counts)

      ;; The QFT should create a specific interference pattern
      (println "\\nQFT creates quantum interference patterns in the computational basis.")
      counts)))

(defn phase-estimation-example
  "Simple phase estimation for a T gate (π/4 phase)."
  []
  (println "\\n=== Quantum Phase Estimation (T gate) ===")

  (let [n-counting 3  ; Counting qubits for precision
        n-total (inc n-counting)  ; Total qubits
        qc (circuit/quantum-circuit n-total)]

    ;; Initialize counting qubits in superposition
    (gates/hadamard qc (range n-counting))

    ;; Initialize target qubit in eigenstate |1⟩ of T gate
    (gates/pauli-x qc n-counting)

    ;; Controlled-U^(2^j) operations where U is T gate
    (doseq [j (range n-counting)]
      (let [control-qubit j
            target-qubit n-counting
            repetitions (Math/pow 2 j)]
        ;; Apply T gate 2^j times (controlled)
        (dotimes [_ repetitions]
          (gates/controlled-phase qc (/ Math/PI 4) control-qubit target-qubit))))

    ;; Inverse QFT on counting qubits
    (let [counting-qubits (range n-counting)]
      ;; For inverse QFT, we reverse the order of operations
      (swap-qubits qc counting-qubits)
      (doseq [i (reverse (range n-counting))]
        (let [qubit (nth counting-qubits i)]
          ;; Controlled rotation gates (in reverse)
          (doseq [j (reverse (range (inc i) n-counting))]
            (let [control-qubit (nth counting-qubits j)
                  k (- j i)
                  angle (- (/ Math/PI (Math/pow 2 k)))]
              (gates/controlled-phase qc angle control-qubit qubit)))
          ;; Hadamard gate
          (gates/hadamard qc qubit))))

    ;; Measure counting qubits only
    (doseq [i (range n-counting)]
      (circuit/measure qc i i))

    (println "Phase estimation circuit:")
    (println (circuit/draw qc))

    (let [counts (backends/execute-circuit qc nil 5000)]
      (println "Phase estimation results:")
      (println counts)

      ;; The T gate has phase π/4, which should be estimated
      (let [most-frequent (key (apply max-key val counts))
            binary-fraction (Integer/parseInt most-frequent 2)
            estimated-phase (* 2 Math/PI (/ binary-fraction (Math/pow 2 n-counting)))]
        (println (format "Most frequent outcome: %s (decimal: %d)" most-frequent binary-fraction))
        (println (format "Estimated phase: %.4f radians (%.4fπ)" estimated-phase (/ estimated-phase Math/PI)))
        (println (format "Actual T gate phase: %.4f radians (%.4fπ)" (/ Math/PI 4) 0.25)))

      counts)))

(defn variational-circuit
  "Create a simple variational quantum circuit (ansatz)."
  [circuit params]
  (let [n-qubits (circuit/num-qubits circuit)
        n-params (count params)]
    (when (not= n-params (* 2 n-qubits))
      (throw (ex-info "Parameter count mismatch"
                      {:expected (* 2 n-qubits) :actual n-params})))

    ;; Layer of RY rotations
    (doseq [i (range n-qubits)]
      (gates/rotation-y circuit (nth params i) i))

    ;; Layer of entangling gates
    (doseq [i (range (dec n-qubits))]
      (gates/cnot circuit i (inc i)))

    ;; Another layer of RY rotations
    (doseq [i (range n-qubits)]
      (gates/rotation-y circuit (nth params (+ n-qubits i)) i))

    circuit))

(defn vqe-example
  "Simple Variational Quantum Eigensolver example for H2 molecule simulation."
  []
  (println "\\n=== Variational Quantum Eigensolver (VQE) ===")

  (let [n-qubits 2
        ;; Simple parameters for demonstration
        params [0.5 0.3 0.7 0.2]  ; 2 * n-qubits parameters

        ;; Create variational circuit
        qc (circuit/quantum-circuit n-qubits)]

    (variational-circuit qc params)

    (println "Variational ansatz circuit:")
    (println (circuit/draw qc))

    ;; For VQE, we would measure expectation values of Pauli operators
    ;; Here we'll just demonstrate the circuit and measure in computational basis
    (circuit/measure-all qc)

    (let [counts (backends/execute-circuit qc nil 3000)]
      (println "State preparation results:")
      (println counts)

      (println "\\nIn a real VQE implementation:")
      (println "1. Measure expectation values of Hamiltonian terms (Pauli operators)")
      (println "2. Use classical optimizer to minimize energy")
      (println "3. Iterate until convergence")

      counts)))

(defn quantum-teleportation
  "Demonstrate quantum teleportation protocol."
  []
  (println "\\n=== Quantum Teleportation ===")

  (let [qc (circuit/quantum-circuit 3 3)  ; 3 qubits, 3 classical bits
        ;; Qubit 0: state to teleport
        ;; Qubit 1: Alice's half of entangled pair
        ;; Qubit 2: Bob's half of entangled pair
        ]

    ;; Prepare state to teleport (arbitrary state: |+⟩)
    (gates/hadamard qc 0)

    ;; Create Bell pair between Alice (qubit 1) and Bob (qubit 2)
    (gates/hadamard qc 1)
    (gates/cnot qc 1 2)

    ;; Bell measurement on qubits 0 and 1
    (gates/cnot qc 0 1)
    (gates/hadamard qc 0)

    ;; Measure Alice's qubits
    (circuit/measure qc 0 0)
    (circuit/measure qc 1 1)

    ;; Classical communication (conditional operations based on measurement)
    ;; In a real implementation, these would be conditional on classical bits
    ;; For simulation, we'll apply all possible corrections

    ;; Note: In actual quantum teleportation, Bob applies corrections based on Alice's measurements
    ;; Here we'll just measure Bob's qubit to see the protocol structure
    (circuit/measure qc 2 2)

    (println "Quantum teleportation circuit:")
    (println (circuit/draw qc))

    (let [counts (backends/execute-circuit qc nil 2000)]
      (println "Teleportation measurement results:")
      (println counts)

      (println "\\nQuantum teleportation protocol:")
      (println "1. Prepare unknown state |ψ⟩ on qubit 0")
      (println "2. Create entangled pair between qubits 1 and 2")
      (println "3. Bell measurement on qubits 0 and 1")
      (println "4. Apply corrections to qubit 2 based on measurement results")
      (println "5. Qubit 2 now contains the original state |ψ⟩")

      counts)))

(defn run-algorithm-examples
  "Run all quantum algorithm examples."
  []
  (println "Running qiskit-clj Quantum Algorithm Examples")
  (println "=============================================\\n")

  (grover-2qubit)
  (qft-example)
  (phase-estimation-example)
  (vqe-example)
  (quantum-teleportation)

  (println "\\n=== Algorithm Examples Complete ==="))

(comment
  ;; Run individual examples:
  (grover-2qubit)
  (qft-example)
  (phase-estimation-example)
  (vqe-example)
  (quantum-teleportation)
  (run-algorithm-examples))

;; If this file is run directly, execute all examples
(when (= *file* (first *command-line-args*))
  (run-algorithm-examples))