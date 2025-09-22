# qiskit-clj

A comprehensive Clojure facade library for [Qiskit](https://qiskit.org/), providing idiomatic Clojure interfaces for quantum computing with IBM's quantum framework.

## Features

- **Idiomatic Clojure API** - Functional programming patterns with threading macros
- **Complete Qiskit Integration** - Full access to Qiskit's quantum circuits, gates, and backends
- **Performance Optimized** - Efficient data conversion between Clojure and Python
- **Comprehensive Documentation** - Rich examples and API documentation
- **Type Safety** - Validation and error handling for quantum operations
- **Modern Tooling** - Built with deps.edn and tools.build

## Installation

Add the following dependency to your `deps.edn`:

```clojure
{:deps {com.justinwoodring/qiskit-clj {:mvn/version \"0.1.0-SNAPSHOT\"}}}
```

### Prerequisites

- **Python 3.8+** with Qiskit installed
- **Java 11+**
- **Clojure 1.12+**

Install Qiskit and optional dependencies:

```bash
pip install qiskit qiskit-aer qiskit-ibm-runtime
```

## Quick Start

```clojure
(require '[com.justinwoodring.qiskit-clj.core :as qiskit]
         '[com.justinwoodring.qiskit-clj.circuit :as circuit]
         '[com.justinwoodring.qiskit-clj.gates :as gates]
         '[com.justinwoodring.qiskit-clj.backends :as backends])

;; Initialize the library
(qiskit/initialize!)

;; Create a Bell state
(def bell-circuit
  (-> (circuit/quantum-circuit 2)
      (gates/hadamard 0)
      (gates/cnot 0 1)
      (circuit/measure-all)))

;; Execute and get results
(def results (backends/execute-circuit bell-circuit))
(println "Bell state results:" results)
```

## Core Modules

### `com.justinwoodring.qiskit-clj.core`
Core utilities for Python integration and error handling.

```clojure
(qiskit/initialize!)                    ; Initialize Python environment
(qiskit/qiskit-version)                ; Get Qiskit version
(qiskit/with-qiskit ...)               ; Execute with environment
```

### `com.justinwoodring.qiskit-clj.circuit`
Quantum circuit construction and manipulation.

```clojure
(def qc (circuit/quantum-circuit 3))   ; Create 3-qubit circuit
(circuit/num-qubits qc)                ; Get qubit count
(circuit/draw qc)                      ; Visualize circuit
```

### `com.justinwoodring.qiskit-clj.gates`
Comprehensive quantum gate library.

```clojure
;; Single-qubit gates
(gates/hadamard circuit [0 1 2])       ; Apply H to multiple qubits
(gates/pauli-x circuit 0)              ; Pauli-X gate
(gates/rotation-y circuit π/4 1)       ; RY rotation

;; Multi-qubit gates
(gates/cnot circuit 0 1)               ; CNOT gate
(gates/toffoli circuit 0 1 2)          ; Toffoli gate
```

### `com.justinwoodring.qiskit-clj.primitives`
Modern Qiskit primitives for quantum execution.

```clojure
;; Sampling
(def sampler (primitives/create-sampler))
(def counts (primitives/sample-circuit circuit 1000))

;; Expectation values
(def estimator (primitives/create-estimator))
(def observable (primitives/pauli-observable \"ZZ\"))
(def expectation (primitives/measure-expectation circuit observable))
```

### `com.justinwoodring.qiskit-clj.backends`
Backend management for simulators and hardware.

```clojure
;; Local simulation
(def aer-backend (backends/aer-simulator))
(def counts (backends/execute-circuit circuit aer-backend))

;; IBM Quantum hardware
(def service (backends/ibm-quantum-service "your-token"))
(def hardware (backends/get-ibm-backend service "ibm_brisbane"))
```

### `com.justinwoodring.qiskit-clj.quantum-info`
Quantum information theory utilities.

```clojure
;; Quantum states
(def bell-state (qi/bell-state :phi-plus))
(def fidelity (qi/state-fidelity state1 state2))

;; Entanglement measures
(def concurrence (qi/concurrence two-qubit-state))
(def entropy (qi/von-neumann-entropy density-matrix))
```

## Examples

### Bell State Creation

```clojure
(def bell-circuit
  (-> (circuit/quantum-circuit 2)
      (gates/hadamard 0)           ; Create superposition
      (gates/cnot 0 1)             ; Entangle qubits
      (circuit/measure-all)))      ; Add measurements

(def results (backends/execute-circuit bell-circuit nil 1000))
;; => {\"00\" 487, \"11\" 513}  ; Should be roughly 50/50
```

### Grover's Search Algorithm

```clojure
(defn grover-search [n-qubits marked-state iterations]
  (let [qc (circuit/quantum-circuit n-qubits)]
    ;; Initialize superposition
    (gates/hadamard qc (range n-qubits))

    ;; Grover iterations
    (dotimes [_ iterations]
      ;; Oracle (marks target state)
      (oracle-function qc marked-state)
      ;; Diffusion operator
      (diffusion-operator qc n-qubits))

    ;; Measure
    (circuit/measure-all qc)
    qc))
```

### Quantum Fourier Transform

```clojure
(defn quantum-fourier-transform [circuit qubits]
  ;; QFT rotations
  (doseq [i (range (count qubits))]
    (gates/hadamard circuit (nth qubits i))
    (doseq [j (range (inc i) (count qubits))]
      (let [angle (/ Math/PI (Math/pow 2 (- j i)))]
        (gates/controlled-phase circuit angle (nth qubits j) (nth qubits i)))))

  ;; Bit reversal
  (let [n (count qubits)]
    (doseq [i (range (quot n 2))]
      (gates/swap-gate circuit (nth qubits i) (nth qubits (- n 1 i)))))

  circuit)
```

## Advanced Usage

### Custom Gate Composition

```clojure
(def custom-gate
  (gates/compose-gates
    [[gates/hadamard [0]]
     [gates/cnot [0 1]]
     [gates/rotation-z [π/4 1]]]))

(custom-gate circuit)
```

### Parameterized Circuits

```clojure
(defn variational-ansatz [circuit params]
  (let [n-qubits (circuit/num-qubits circuit)]
    ;; RY rotation layer
    (doseq [i (range n-qubits)]
      (gates/rotation-y circuit (nth params i) i))

    ;; Entangling layer
    (doseq [i (range (dec n-qubits))]
      (gates/cnot circuit i (inc i)))

    ;; Second RY layer
    (doseq [i (range n-qubits)]
      (gates/rotation-y circuit (nth params (+ n-qubits i)) i))

    circuit))
```

### Noise Modeling

```clojure
(def noise-model (backends/noise-model))
(def depol-error (backends/depolarizing-error 0.01 1))
(backends/add-quantum-error noise-model depol-error ["x" "h"])

(def noisy-backend
  (backends/aer-simulator {:noise_model noise-model}))
```

## Testing

Run the test suite:

```bash
clojure -M:test
```

Run specific test namespaces:

```bash
clojure -M:test -n com.justinwoodring.qiskit-clj.core-test
```

## Examples

Explore comprehensive examples:

```bash
# Basic examples
clojure -M examples/basic_examples.clj

# Quantum algorithms
clojure -M examples/quantum_algorithms.clj
```

## Building

Build the library:

```bash
clojure -T:build jar
```

Install locally:

```bash
clojure -T:build install
```

## API Documentation

Generate API docs:

```bash
clojure -M:docs
```

## Performance

The library uses libpython-clj's zero-copy data transfer for numpy arrays and optimized conversion functions for maximum performance. Benchmark your specific use case, but typical overhead is minimal.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## IBM Quantum Access

To use IBM Quantum hardware:

1. Create an IBM Quantum account at [quantum.ibm.com](https://quantum.ibm.com)
2. Get your API token from your account settings
3. Use the token with the backends module:

```clojure
(def service (backends/ibm-quantum-service "your-token-here"))
(def backend (backends/get-ibm-backend service "ibm_brisbane"))
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/justinwoodring/qiskit-clj/issues)
- **Discussions**: [GitHub Discussions](https://github.com/justinwoodring/qiskit-clj/discussions)
- **Documentation**: [API Docs](https://justinwoodring.github.io/qiskit-clj/)

## Acknowledgments

- [Qiskit](https://qiskit.org/) - IBM's quantum computing framework
- [libpython-clj](https://github.com/clj-python/libpython-clj) - Python interop for Clojure
- The Clojure and quantum computing communities

---

*qiskit-clj brings the power of quantum computing to the Clojure ecosystem with idiomatic, functional programming patterns.*
