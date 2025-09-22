(ns com.justinwoodring.qiskit-clj.gates
  "Quantum gate library with idiomatic Clojure wrappers.

   This namespace provides comprehensive gate operations for quantum circuits,
   including single-qubit gates, multi-qubit gates, and parameterized gates."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]))

(defmacro safe-gate-op
  "Macro to wrap gate operations with safety checks and error handling."
  [circuit qubits & body]
  `(core/with-python-context
     (try
       (let [qubit-list# (if (coll? ~qubits) ~qubits [~qubits])
             num-qubits# (py/get-attr ~circuit "num_qubits")]
         (doseq [q# qubit-list#]
           (core/validate-qubit-index q# num-qubits#))
         ~@body
         ~circuit)
       (catch Exception e#
         (throw (ex-info "Gate operation failed"
                         {:circuit (str ~circuit)
                          :qubits ~qubits
                          :error (str e#)}
                         e#))))))

;; Single-qubit Pauli gates
(defn pauli-x
  "Apply Pauli-X gate to specified qubits."
  [circuit qubits]
  (safe-gate-op circuit qubits
    (doseq [q (if (coll? qubits) qubits [qubits])]
      (py/call-attr circuit "x" q))))

(defn pauli-y
  "Apply Pauli-Y gate to specified qubits."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "y" q))
    circuit))

(defn pauli-z
  "Apply Pauli-Z gate to specified qubits."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "z" q))
    circuit))

;; Hadamard and phase gates
(defn hadamard
  "Apply Hadamard gate to specified qubits."
  [circuit qubits]
  (safe-gate-op circuit qubits
    (doseq [q (if (coll? qubits) qubits [qubits])]
      (py/call-attr circuit "h" q))))

(defn s-gate
  "Apply S gate (phase gate) to specified qubits."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "s" q))
    circuit))

(defn s-dagger
  "Apply S† (inverse S gate) to specified qubits."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "sdg" q))
    circuit))

(defn t-gate
  "Apply T gate to specified qubits."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "t" q))
    circuit))

(defn t-dagger
  "Apply T† (inverse T gate) to specified qubits."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "tdg" q))
    circuit))

;; Rotation gates
(defn rotation-x
  "Apply rotation around X-axis.

   Args:
   - circuit: QuantumCircuit
   - theta: Rotation angle in radians
   - qubits: Target qubit(s)"
  [circuit theta qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "rx" theta q))
    circuit))

(defn rotation-y
  "Apply rotation around Y-axis."
  [circuit theta qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "ry" theta q))
    circuit))

(defn rotation-z
  "Apply rotation around Z-axis."
  [circuit theta qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "rz" theta q))
    circuit))

(defn phase-gate
  "Apply phase gate with angle phi.

   Args:
   - circuit: QuantumCircuit
   - phi: Phase angle in radians
   - qubits: Target qubit(s)"
  [circuit phi qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "p" phi q))
    circuit))

;; U gates (universal single-qubit gates)
(defn u1-gate
  "Apply U1 gate (equivalent to phase gate)."
  [circuit lambda qubits]
  (phase-gate circuit lambda qubits))

(defn u2-gate
  "Apply U2 gate.

   U2(φ,λ) = RZ(φ) RY(π/2) RZ(λ)"
  [circuit phi lambda qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "u2" phi lambda q))
    circuit))

(defn u3-gate
  "Apply U3 gate (most general single-qubit gate).

   U3(θ,φ,λ) = RZ(φ) RY(θ) RZ(λ)"
  [circuit theta phi lambda qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "u3" theta phi lambda q))
    circuit))

;; Two-qubit gates
(defn cnot
  "Apply CNOT (controlled-X) gate."
  [circuit control target]
  (core/with-python-context
    (try
      (let [num-qubits (py/get-attr circuit "num_qubits")]
        (core/validate-qubit-index control num-qubits)
        (core/validate-qubit-index target num-qubits)
        (py/call-attr circuit "cx" control target)
        circuit)
      (catch Exception e
        (throw (ex-info "CNOT gate operation failed"
                        {:control control :target target :error (str e)}
                        e))))))

(defn controlled-y
  "Apply controlled-Y gate."
  [circuit control target]
  (py/call-attr circuit "cy" control target)
  circuit)

(defn controlled-z
  "Apply controlled-Z gate."
  [circuit control target]
  (py/call-attr circuit "cz" control target)
  circuit)

(defn controlled-hadamard
  "Apply controlled-Hadamard gate."
  [circuit control target]
  (py/call-attr circuit "ch" control target)
  circuit)

(defn swap-gate
  "Apply SWAP gate."
  [circuit qubit1 qubit2]
  (py/call-attr circuit "swap" qubit1 qubit2)
  circuit)

(defn iswap-gate
  "Apply iSWAP gate."
  [circuit qubit1 qubit2]
  (py/call-attr circuit "iswap" qubit1 qubit2)
  circuit)

;; Controlled rotation gates
(defn controlled-rx
  "Apply controlled rotation around X-axis."
  [circuit theta control target]
  (py/call-attr circuit "crx" theta control target)
  circuit)

(defn controlled-ry
  "Apply controlled rotation around Y-axis."
  [circuit theta control target]
  (py/call-attr circuit "cry" theta control target)
  circuit)

(defn controlled-rz
  "Apply controlled rotation around Z-axis."
  [circuit theta control target]
  (py/call-attr circuit "crz" theta control target)
  circuit)

(defn controlled-phase
  "Apply controlled phase gate."
  [circuit phi control target]
  (py/call-attr circuit "cp" phi control target)
  circuit)

;; Three-qubit gates
(defn toffoli
  "Apply Toffoli (CCNOT) gate.

   Args:
   - circuit: QuantumCircuit
   - control1: First control qubit
   - control2: Second control qubit
   - target: Target qubit"
  [circuit control1 control2 target]
  (py/call-attr circuit "ccx" control1 control2 target)
  circuit)

(defn fredkin
  "Apply Fredkin (controlled-SWAP) gate.

   Args:
   - circuit: QuantumCircuit
   - control: Control qubit
   - target1: First target qubit
   - target2: Second target qubit"
  [circuit control target1 target2]
  (py/call-attr circuit "cswap" control target1 target2)
  circuit)

;; Special gates
(defn identity-gate
  "Apply identity gate (no-op) to specified qubits."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "id" q))
    circuit))

(defn reset-gate
  "Reset specified qubits to |0⟩ state."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (py/call-attr circuit "reset" q))
    circuit))

;; Multi-controlled gates
(defn multi-controlled-x
  "Apply multi-controlled X gate.

   Args:
   - circuit: QuantumCircuit
   - controls: Collection of control qubits
   - target: Target qubit"
  [circuit controls target]
  (core/with-qiskit
    (let [qlib (py/import-module "qiskit.circuit.library")
          mcx-gate (py/call-attr qlib "MCXGate" (count controls))]
      (py/call-attr circuit "append" mcx-gate (concat controls [target])))
    circuit))

(defn multi-controlled-z
  "Apply multi-controlled Z gate."
  [circuit controls target]
  (core/with-qiskit
    (let [qlib (py/import-module "qiskit.circuit.library")
          mcz-gate (py/call-attr qlib "MCZGate" (count controls))]
      (py/call-attr circuit "append" mcz-gate (concat controls [target])))
    circuit))

;; Parameterized gates
(defn parameterized-gate
  "Create a parameterized gate.

   Args:
   - gate-fn: Function that takes parameters and returns a gate
   - param-names: Vector of parameter names (keywords)

   Returns: Function that takes circuit, parameters, and qubits"
  [gate-fn param-names]
  (fn [circuit params qubits]
    (let [param-map (zipmap param-names params)]
      (gate-fn circuit param-map qubits))))

;; Common parameterized gate constructors
(defn rx-param
  "Create parameterized RX gate."
  [param-name]
  (fn [circuit theta qubits]
    (rotation-x circuit theta qubits)))

(defn ry-param
  "Create parameterized RY gate."
  [param-name]
  (fn [circuit theta qubits]
    (rotation-y circuit theta qubits)))

(defn rz-param
  "Create parameterized RZ gate."
  [param-name]
  (fn [circuit theta qubits]
    (rotation-z circuit theta qubits)))

;; Custom gate composition
(defn compose-gates
  "Compose multiple gates into a single operation.

   Args:
   - gates: Vector of [gate-fn args] pairs

   Returns: Function that applies all gates in sequence"
  [gates]
  (fn [circuit]
    (reduce (fn [c [gate-fn & args]]
              (apply gate-fn c args))
            circuit
            gates)))

;; Gate decomposition helpers
(defn decompose-to-basis
  "Decompose circuit gates to a specific basis set.

   Common basis sets:
   - [:x :y :z :cnot] - Pauli + CNOT
   - [:rx :ry :rz :cnot] - Rotation + CNOT
   - [:h :t :cnot] - Clifford + T"
  [circuit basis-gates]
  (core/with-qiskit
    (let [transpiler (py/import-module "qiskit.transpiler")
          transpiler-passes (py/import-module "qiskit.transpiler.passes")
          pass-manager (py/call-attr transpiler "PassManager")]
      (py/call-attr pass-manager "append"
                    (py/call-attr transpiler-passes "Unroller" (core/->py basis-gates)))
      (py/call-attr pass-manager "run" circuit))))

;; Gate verification and properties
(defn is-unitary?
  "Check if a gate is unitary."
  [gate]
  (core/with-qiskit
    (try
      (py/call-attr gate "to_matrix")
      true
      (catch Exception _ false))))

(defn gate-matrix
  "Get the matrix representation of a gate."
  [gate]
  (core/->clj (py/call-attr gate "to_matrix")))

(defn gate-power
  "Raise a gate to a power.

   Args:
   - gate: Gate object
   - power: Exponent (can be fractional)

   Returns: New gate raised to the power"
  [gate power]
  (py/call-attr gate "power" power))

(comment
  ;; Example usage
  (require '[com.justinwoodring.qiskit-clj.core :as core])
  (require '[com.justinwoodring.qiskit-clj.circuit :as circuit])
  (core/initialize!)

  ;; Create Bell state using gates
  (-> (circuit/quantum-circuit 2)
      (hadamard 0)
      (cnot 0 1)
      (circuit/measure-all)
      (circuit/draw))

  ;; Parameterized circuit example
  (let [theta (/ Math/PI 4)]
    (-> (circuit/quantum-circuit 1)
        (rotation-y theta 0)
        (circuit/draw)))

  ;; Multi-controlled gate example
  (-> (circuit/quantum-circuit 4)
      (hadamard [0 1 2])
      (multi-controlled-x [0 1 2] 3)
        (circuit/draw)))