(ns com.justinwoodring.qiskit-clj.circuit
  "Quantum circuit construction and manipulation.

   This namespace provides idiomatic Clojure functions for creating and
   manipulating quantum circuits using Qiskit's QuantumCircuit class."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]))

(defn quantum-circuit
  "Create a new quantum circuit.

   Args:
   - n-qubits: Number of quantum bits (required)
   - n-classical: Number of classical bits (optional, defaults to n-qubits)
   - name: Circuit name (optional)

   Returns: QuantumCircuit object

   Example:
   (quantum-circuit 3)
   (quantum-circuit 3 3 \"my-circuit\")"
  ([n-qubits]
   (quantum-circuit n-qubits n-qubits nil))
  ([n-qubits n-classical]
   (quantum-circuit n-qubits n-classical nil))
  ([n-qubits n-classical name]
   (core/with-qiskit
     (core/validate-qubit-count n-qubits)
     (require-python '[qiskit :as qk])
     (let [qc (py/call-attr (py/import-module "qiskit") "QuantumCircuit"
                            n-qubits n-classical)]
       (when name
         (py/set-attr! qc "name" name))
       qc))))

(defn copy-circuit
  "Create a copy of an existing quantum circuit."
  [circuit]
  (py/call-attr circuit "copy"))

(defn num-qubits
  "Get the number of qubits in a circuit."
  [circuit]
  (py/get-attr circuit "num_qubits"))

(defn num-clbits
  "Get the number of classical bits in a circuit."
  [circuit]
  (py/get-attr circuit "num_clbits"))

(defn circuit-depth
  "Get the depth of the circuit."
  [circuit]
  (py/call-attr circuit "depth"))

(defn circuit-size
  "Get the number of operations in the circuit."
  [circuit]
  (py/call-attr circuit "size"))

(defn circuit-width
  "Get the total width (qubits + classical bits) of the circuit."
  [circuit]
  (py/call-attr circuit "width"))

(defn add-gate
  "Add a gate to the circuit.

   Args:
   - circuit: QuantumCircuit object
   - gate: Gate object or gate name
   - qubits: Qubit indices (can be single index or collection)
   - params: Gate parameters (optional)

   Returns: Modified circuit (for threading)"
  ([circuit gate qubits]
   (add-gate circuit gate qubits nil))
  ([circuit gate qubits params]
   (let [qubit-list (if (coll? qubits) qubits [qubits])
         gate-obj (if (string? gate)
                    (py/get-attr (py/import-module "qiskit.circuit.library") gate)
                    gate)]
     (if params
       (py/call-attr circuit "append" (apply gate-obj params) qubit-list)
       (py/call-attr circuit "append" gate-obj qubit-list))
     circuit)))

(defn h
  "Apply Hadamard gate to qubit(s).

   Args:
   - circuit: QuantumCircuit object
   - qubits: Qubit index or collection of indices

   Returns: Modified circuit"
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (core/validate-qubit-index q (num-qubits circuit))
      (py/call-attr circuit "h" q))
    circuit))

(defn x
  "Apply Pauli-X gate to qubit(s)."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (core/validate-qubit-index q (num-qubits circuit))
      (py/call-attr circuit "x" q))
    circuit))

(defn y
  "Apply Pauli-Y gate to qubit(s)."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (core/validate-qubit-index q (num-qubits circuit))
      (py/call-attr circuit "y" q))
    circuit))

(defn z
  "Apply Pauli-Z gate to qubit(s)."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (core/validate-qubit-index q (num-qubits circuit))
      (py/call-attr circuit "z" q))
    circuit))

(defn cx
  "Apply CNOT (controlled-X) gate.

   Args:
   - circuit: QuantumCircuit object
   - control: Control qubit index
   - target: Target qubit index

   Returns: Modified circuit"
  [circuit control target]
  (core/validate-qubit-index control (num-qubits circuit))
  (core/validate-qubit-index target (num-qubits circuit))
  (py/call-attr circuit "cx" control target)
  circuit)

(defn cz
  "Apply controlled-Z gate."
  [circuit control target]
  (core/validate-qubit-index control (num-qubits circuit))
  (core/validate-qubit-index target (num-qubits circuit))
  (py/call-attr circuit "cz" control target)
  circuit)

(defn rx
  "Apply rotation around X-axis.

   Args:
   - circuit: QuantumCircuit
   - theta: Rotation angle in radians
   - qubit: Target qubit index"
  [circuit theta qubit]
  (core/validate-qubit-index qubit (num-qubits circuit))
  (py/call-attr circuit "rx" theta qubit)
  circuit)

(defn ry
  "Apply rotation around Y-axis."
  [circuit theta qubit]
  (core/validate-qubit-index qubit (num-qubits circuit))
  (py/call-attr circuit "ry" theta qubit)
  circuit)

(defn rz
  "Apply rotation around Z-axis."
  [circuit theta qubit]
  (core/validate-qubit-index qubit (num-qubits circuit))
  (py/call-attr circuit "rz" theta qubit)
  circuit)

(defn measure
  "Add measurement operation.

   Args:
   - circuit: QuantumCircuit
   - qubit: Qubit index to measure
   - clbit: Classical bit index to store result

   Returns: Modified circuit"
  [circuit qubit clbit]
  (core/validate-qubit-index qubit (num-qubits circuit))
  (py/call-attr circuit "measure" qubit clbit)
  circuit)

(defn measure-all
  "Add measurements for all qubits to corresponding classical bits."
  [circuit]
  (py/call-attr circuit "measure_all")
  circuit)

(defn barrier
  "Add a barrier to the circuit.

   Args:
   - circuit: QuantumCircuit
   - qubits: Qubit indices (optional, defaults to all qubits)"
  ([circuit]
   (py/call-attr circuit "barrier")
   circuit)
  ([circuit qubits]
   (let [qubit-list (if (coll? qubits) qubits [qubits])]
     (py/call-attr circuit "barrier" qubit-list)
     circuit)))

(defn reset
  "Reset qubit(s) to |0⟩ state."
  [circuit qubits]
  (let [qubit-list (if (coll? qubits) qubits [qubits])]
    (doseq [q qubit-list]
      (core/validate-qubit-index q (num-qubits circuit))
      (py/call-attr circuit "reset" q))
    circuit))

(defn compose
  "Compose two circuits.

   Args:
   - circuit1: First circuit
   - circuit2: Second circuit to append
   - qubits: Qubit mapping (optional)

   Returns: New composed circuit"
  ([circuit1 circuit2]
   (py/call-attr circuit1 "compose" circuit2))
  ([circuit1 circuit2 qubits]
   (py/call-attr circuit1 "compose" circuit2 :qubits qubits)))

(defn reverse-ops
  "Reverse the circuit operations."
  [circuit]
  (py/call-attr circuit "reverse_ops"))

(defn inverse
  "Create the inverse of the circuit."
  [circuit]
  (py/call-attr circuit "inverse"))

(defn draw
  "Draw the circuit.

   Options:
   - :output - Output format (:text, :mpl, :latex)
   - :scale - Scale factor for matplotlib output
   - :filename - File to save the drawing

   Returns: Circuit drawing string or saves to file"
  ([circuit]
   (draw circuit {}))
  ([circuit {:keys [output scale filename] :or {output :text}}]
   (try
     (let [output-str (name output)
           kwargs (cond-> {:output output-str}
                    scale (assoc :scale scale)
                    filename (assoc :filename filename))]
       (str (py/call-attr circuit "draw" kwargs)))
     (catch Exception e
       (str "Circuit with " (num-qubits circuit) " qubits and "
            (circuit-size circuit) " operations")))))

(defn print-circuit
  "Safely print a circuit with basic information.

   Returns: String representation of the circuit"
  [circuit]
  (try
    (draw circuit {:output :text})
    (catch Exception e
      (str "QuantumCircuit("
           "qubits=" (num-qubits circuit)
           ", depth=" (circuit-depth circuit)
           ", size=" (circuit-size circuit)
           ")"))))

(defn qasm
  "Get QASM representation of the circuit."
  [circuit]
  (py/call-attr circuit "qasm"))

(defn parameters
  "Get the parameters in the circuit."
  [circuit]
  (core/->clj (py/get-attr circuit "parameters")))

(defn bind-parameters
  "Bind values to circuit parameters.

   Args:
   - circuit: QuantumCircuit with parameters
   - param-map: Map of parameter->value bindings

   Returns: New circuit with bound parameters"
  [circuit param-map]
  (py/call-attr circuit "bind_parameters" (core/->py param-map)))

(comment
  ;; Example usage
  (require '[com.justinwoodring.qiskit-clj.core :as core])
  (core/initialize!)

  ;; Create a simple Bell state circuit
  (-> (quantum-circuit 2)
      (h 0)
      (cx 0 1)
      (measure-all)
      (draw))

  ;; Create a more complex circuit with threading
  (-> (quantum-circuit 3 3 "ghz-state")
      (h 0)
      (cx 0 1)
      (cx 1 2)
      (barrier)
      (measure-all)
      (draw)))