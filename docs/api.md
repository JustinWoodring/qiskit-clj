# qiskit-clj API Documentation

This document provides detailed API documentation for all qiskit-clj modules.

## Table of Contents

- [Core Module](#core-module)
- [Circuit Module](#circuit-module)
- [Gates Module](#gates-module)
- [Primitives Module](#primitives-module)
- [Backends Module](#backends-module)
- [Quantum Info Module](#quantum-info-module)

## Core Module

`com.justinwoodring.qiskit-clj.core`

### Initialization Functions

#### `initialize!`
```clojure
(initialize!)
(initialize! {:keys [python-executable python-library-path]})
```
Initialize the Python environment and import Qiskit.

**Parameters:**
- `:python-executable` - Path to Python executable (optional)
- `:python-library-path` - Path to Python library (optional)

**Example:**
```clojure
(initialize!)
(initialize! {:python-executable \"/opt/conda/bin/python\"})
```

#### `ensure-initialized!`
```clojure
(ensure-initialized!)
```
Ensure Python environment is initialized, initialize if not.

### Data Conversion

#### `->clj`
```clojure
(->clj py-obj)
```
Convert Python object to Clojure data structure.

#### `->py`
```clojure
(->py clj-obj)
```
Convert Clojure data structure to Python object.

#### `py-call`
```clojure
(py-call py-fn & args)
```
Call a Python function with Clojure arguments. Arguments are automatically converted.

### Validation Functions

#### `validate-qubit-count`
```clojure
(validate-qubit-count n)
```
Validate that qubit count is positive integer.

#### `validate-qubit-index`
```clojure
(validate-qubit-index idx max-qubits)
```
Validate that qubit index is within range [0, max-qubits).

### Error Handling

#### `qiskit-error?`
```clojure
(qiskit-error? ex)
```
Check if an exception is a Qiskit-related error.

#### `with-qiskit-error-handling`
```clojure
(with-qiskit-error-handling & body)
```
Execute body with Qiskit-specific error handling.

---

## Circuit Module

`com.justinwoodring.qiskit-clj.circuit`

### Circuit Creation

#### `quantum-circuit`
```clojure
(quantum-circuit n-qubits)
(quantum-circuit n-qubits n-classical)
(quantum-circuit n-qubits n-classical name)
```
Create a new quantum circuit.

**Parameters:**
- `n-qubits` - Number of quantum bits (required)
- `n-classical` - Number of classical bits (optional, defaults to n-qubits)
- `name` - Circuit name (optional)

**Example:**
```clojure
(quantum-circuit 3)
(quantum-circuit 3 2 \"my-circuit\")
```

### Circuit Properties

#### `num-qubits`
```clojure
(num-qubits circuit)
```
Get the number of qubits in a circuit.

#### `num-clbits`
```clojure
(num-clbits circuit)
```
Get the number of classical bits in a circuit.

#### `circuit-depth`
```clojure
(circuit-depth circuit)
```
Get the depth of the circuit.

#### `circuit-size`
```clojure
(circuit-size circuit)
```
Get the number of operations in the circuit.

### Basic Gates

#### `h`
```clojure
(h circuit qubits)
```
Apply Hadamard gate to qubit(s).

#### `x`, `y`, `z`
```clojure
(x circuit qubits)
(y circuit qubits)
(z circuit qubits)
```
Apply Pauli gates to qubit(s).

#### `cx`
```clojure
(cx circuit control target)
```
Apply CNOT gate.

#### `rx`, `ry`, `rz`
```clojure
(rx circuit theta qubit)
(ry circuit theta qubit)
(rz circuit theta qubit)
```
Apply rotation gates around X, Y, Z axes.

### Measurement

#### `measure`
```clojure
(measure circuit qubit clbit)
```
Add measurement operation.

#### `measure-all`
```clojure
(measure-all circuit)
```
Add measurements for all qubits to corresponding classical bits.

### Circuit Operations

#### `barrier`
```clojure
(barrier circuit)
(barrier circuit qubits)
```
Add a barrier to the circuit.

#### `reset`
```clojure
(reset circuit qubits)
```
Reset qubit(s) to |0⟩ state.

#### `copy-circuit`
```clojure
(copy-circuit circuit)
```
Create a copy of an existing quantum circuit.

#### `compose`
```clojure
(compose circuit1 circuit2)
(compose circuit1 circuit2 qubits)
```
Compose two circuits.

### Visualization

#### `draw`
```clojure
(draw circuit)
(draw circuit {:keys [output scale filename]})
```
Draw the circuit with various output formats.

---

## Gates Module

`com.justinwoodring.qiskit-clj.gates`

### Single-Qubit Gates

#### Pauli Gates
```clojure
(pauli-x circuit qubits)
(pauli-y circuit qubits)
(pauli-z circuit qubits)
```

#### Hadamard and Phase Gates
```clojure
(hadamard circuit qubits)
(s-gate circuit qubits)
(s-dagger circuit qubits)
(t-gate circuit qubits)
(t-dagger circuit qubits)
```

#### Rotation Gates
```clojure
(rotation-x circuit theta qubits)
(rotation-y circuit theta qubits)
(rotation-z circuit theta qubits)
(phase-gate circuit phi qubits)
```

#### Universal Single-Qubit Gates
```clojure
(u1-gate circuit lambda qubits)
(u2-gate circuit phi lambda qubits)
(u3-gate circuit theta phi lambda qubits)
```

### Two-Qubit Gates

```clojure
(cnot circuit control target)
(controlled-y circuit control target)
(controlled-z circuit control target)
(controlled-hadamard circuit control target)
(swap-gate circuit qubit1 qubit2)
(iswap-gate circuit qubit1 qubit2)
```

#### Controlled Rotation Gates
```clojure
(controlled-rx circuit theta control target)
(controlled-ry circuit theta control target)
(controlled-rz circuit theta control target)
(controlled-phase circuit phi control target)
```

### Three-Qubit Gates

```clojure
(toffoli circuit control1 control2 target)
(fredkin circuit control target1 target2)
```

### Multi-Controlled Gates

```clojure
(multi-controlled-x circuit controls target)
(multi-controlled-z circuit controls target)
```

---

## Primitives Module

`com.justinwoodring.qiskit-clj.primitives`

### Sampler Primitive

#### `create-sampler`
```clojure
(create-sampler)
(create-sampler {:keys [backend options]})
```
Create a Sampler primitive for quantum circuit sampling.

#### `run-sampler`
```clojure
(run-sampler sampler circuits)
(run-sampler sampler circuits parameter-values)
(run-sampler sampler circuits parameter-values shots)
```
Run circuits using the Sampler primitive.

#### `sample-circuit`
```clojure
(sample-circuit circuit)
(sample-circuit circuit shots)
(sample-circuit circuit shots backend)
```
High-level function to sample a quantum circuit.

### Estimator Primitive

#### `create-estimator`
```clojure
(create-estimator)
(create-estimator {:keys [backend options]})
```
Create an Estimator primitive for observable expectation values.

#### `run-estimator`
```clojure
(run-estimator estimator circuits observables)
(run-estimator estimator circuits observables parameter-values)
(run-estimator estimator circuits observables parameter-values shots)
```
Run circuits with observables using the Estimator primitive.

#### `measure-expectation`
```clojure
(measure-expectation circuit observable)
(measure-expectation circuit observable shots)
(measure-expectation circuit observable shots backend)
```
High-level function to measure expectation value.

### Observable Construction

#### `pauli-observable`
```clojure
(pauli-observable pauli-string)
(pauli-observable pauli-string coefficient)
```
Create a Pauli observable from a string.

#### `pauli-sum`
```clojure
(pauli-sum pauli-terms)
```
Create a sum of Pauli observables.

#### `z-observable`, `x-observable`, `y-observable`
```clojure
(z-observable n-qubits target-qubits)
(x-observable n-qubits target-qubits)
(y-observable n-qubits target-qubits)
```
Create Pauli observables for specified qubits.

---

## Backends Module

`com.justinwoodring.qiskit-clj.backends`

### Simulator Backends

#### `aer-simulator`
```clojure
(aer-simulator)
(aer-simulator {:keys [method device shots max-parallel-threads]})
```
Create an Aer simulator backend.

#### `basic-simulator`
```clojure
(basic-simulator)
```
Create a basic simulator backend (no Aer required).

#### `fake-backend`
```clojure
(fake-backend backend-name)
```
Create a fake backend for testing.

### IBM Quantum Backends

#### `ibm-quantum-service`
```clojure
(ibm-quantum-service)
(ibm-quantum-service token)
(ibm-quantum-service token instance)
```
Create IBM Quantum service for accessing real hardware.

#### `list-ibm-backends`
```clojure
(list-ibm-backends)
(list-ibm-backends service)
(list-ibm-backends service operational-only)
```
List available IBM Quantum backends.

#### `get-ibm-backend`
```clojure
(get-ibm-backend service backend-name)
```
Get a specific IBM Quantum backend.

### Backend Information

#### `backend-name`, `backend-version`, `backend-status`
```clojure
(backend-name backend)
(backend-version backend)
(backend-status backend)
```

#### `backend-configuration`, `backend-properties`
```clojure
(backend-configuration backend)
(backend-properties backend)
```

#### `num-qubits`, `coupling-map`, `basis-gates`
```clojure
(num-qubits backend)
(coupling-map backend)
(basis-gates backend)
```

### Circuit Execution

#### `execute-circuit`
```clojure
(execute-circuit circuit)
(execute-circuit circuit backend)
(execute-circuit circuit backend shots)
(execute-circuit circuit backend shots transpile?)
```
High-level function to execute a circuit and get results.

#### `run-circuit`
```clojure
(run-circuit backend circuits)
(run-circuit backend circuits shots)
(run-circuit backend circuits shots memory)
```
Run a circuit on a backend (returns Job object).

### Transpilation

#### `transpile`
```clojure
(transpile circuits backend)
(transpile circuits backend {:keys [optimization-level initial-layout seed-transpiler]})
```
Transpile circuits for a specific backend.

---

## Quantum Info Module

`com.justinwoodring.qiskit-clj.quantum-info`

### Quantum State Creation

#### `statevector`
```clojure
(statevector data)
(statevector data validate)
```
Create a quantum state vector.

#### `zero-state`, `plus-state`
```clojure
(zero-state n-qubits)
(plus-state n-qubits)
```
Create standard quantum states.

#### `bell-state`
```clojure
(bell-state)
(bell-state bell-type)
```
Create Bell states. Types: `:phi-plus`, `:phi-minus`, `:psi-plus`, `:psi-minus`.

#### `ghz-state`, `w-state`
```clojure
(ghz-state n-qubits)
(w-state n-qubits)
```
Create multi-qubit entangled states.

### State Properties

#### `state-vector-data`, `state-dims`
```clojure
(state-vector-data state)
(state-dims state)
```

#### `probabilities`, `probability`
```clojure
(probabilities state)
(probability state outcome)
```

#### `sample-memory`, `sample-counts`
```clojure
(sample-memory state shots)
(sample-counts state shots)
```

### Density Matrix Operations

#### `density-matrix`
```clojure
(density-matrix data)
```
Create a density matrix.

#### `partial-trace`
```clojure
(partial-trace rho qubits-to-trace)
```
Compute partial trace of a density matrix.

#### `purity`, `von-neumann-entropy`
```clojure
(purity rho)
(von-neumann-entropy rho)
(von-neumann-entropy rho base)
```

### Fidelity and Distance Measures

#### `state-fidelity`
```clojure
(state-fidelity state1 state2)
```
Calculate fidelity between two quantum states.

#### `trace-distance`
```clojure
(trace-distance state1 state2)
```
Calculate trace distance between two states.

### Entanglement Measures

#### `concurrence`
```clojure
(concurrence state)
```
Calculate concurrence for a two-qubit state.

#### `entanglement-of-formation`
```clojure
(entanglement-of-formation state partition)
```
Calculate entanglement of formation for a bipartite state.

### Random Quantum Objects

#### `random-statevector`, `random-density-matrix`, `random-unitary`
```clojure
(random-statevector dims)
(random-statevector dims seed)
(random-density-matrix dims)
(random-unitary dims)
```
Generate random quantum objects for testing.

---

## Common Patterns

### Threading Macros

All circuit operations return the circuit object, enabling threading:

```clojure
(-> (circuit/quantum-circuit 3)
    (gates/hadamard 0)
    (gates/cnot 0 1)
    (gates/cnot 1 2)
    (circuit/barrier)
    (circuit/measure-all)
    (backends/execute-circuit))
```

### Error Handling

Use `with-qiskit-error-handling` for Qiskit-specific error processing:

```clojure
(core/with-qiskit-error-handling
  (backends/execute-circuit invalid-circuit))
```

### Batch Operations

Many functions accept both single items and collections:

```clojure
(gates/hadamard circuit 0)        ; Single qubit
(gates/hadamard circuit [0 1 2])  ; Multiple qubits
```