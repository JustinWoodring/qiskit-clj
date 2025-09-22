(ns com.justinwoodring.qiskit-clj.backends
  "Backend management for quantum hardware and simulators.

   This namespace provides utilities for working with quantum backends,
   including local simulators, IBM Quantum hardware, and transpilation."
  (:require [com.justinwoodring.qiskit-clj.core :as core]
            [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]))

;; Basic simulator backends
(defn aer-simulator
  "Create an Aer simulator backend.

   Options:
   - :method - Simulation method (:statevector, :unitary, :density_matrix, etc.)
   - :device - Device type (:cpu, :gpu)
   - :shots - Default number of shots
   - :max-parallel-threads - Number of parallel threads

   Returns: AerSimulator backend"
  ([]
   (aer-simulator {}))
  ([{:keys [method device shots max-parallel-threads]}]
   (core/with-qiskit
     (try
       (let [aer (py/import-module "qiskit_aer")]
         (if (or method device shots max-parallel-threads)
           (py/call-attr-kw aer "AerSimulator" []
                           (cond-> {}
                             method (assoc :method (name method))
                             device (assoc :device (name device))
                             shots (assoc :shots shots)
                             max-parallel-threads (assoc :max_parallel_threads max-parallel-threads)))
           (py/call-attr aer "AerSimulator")))
       (catch Exception e
         (throw (ex-info "Qiskit Aer not available. Install with: pip install qiskit-aer"
                         {:error (str e)})))))))

(defn basic-simulator
  "Create a basic simulator backend (no Aer required).

   Returns: BasicSimulator backend"
  []
  (core/with-qiskit
    (let [basic (py/import-module "qiskit.providers.basic_provider")]
      (py/call-attr basic "BasicSimulator"))))

(defn fake-backend
  "Create a fake backend for testing (simulates real hardware).

   Args:
   - backend-name: Name of the backend to simulate

   Common backends: :fake_manila, :fake_cairo, :fake_kolkata, :fake_mumbai

   Returns: Fake backend"
  [backend-name]
  (core/with-qiskit
    (try
      (let [fake (py/import-module "qiskit.providers.fake_provider")
            backend-class-name (str "Fake" (apply str (map #(if (= % \-)
                                                              ""
                                                              (Character/toUpperCase %))
                                                           (name backend-name))))]
        (py/get-attr fake backend-class-name))
      (catch Exception e
        (throw (ex-info "Fake backend not available"
                        {:backend backend-name :error (str e)}))))))

;; IBM Quantum backends
(defn ibm-quantum-service
  "Create IBM Quantum service for accessing real hardware.

   Args:
   - token: IBM Quantum token (optional if saved)
   - instance: IBM Quantum instance (optional)

   Returns: QiskitRuntimeService"
  ([]
   (ibm-quantum-service nil nil))
  ([token]
   (ibm-quantum-service token nil))
  ([token instance]
   (core/with-qiskit
     (try
       (let [ibm (py/import-module "qiskit_ibm_runtime")]
         (if (or token instance)
           (py/call-attr-kw ibm "QiskitRuntimeService" []
                           (cond-> {}
                             token (assoc :token token)
                             instance (assoc :instance instance)))
           (py/call-attr ibm "QiskitRuntimeService")))
       (catch Exception e
         (throw (ex-info "IBM Quantum Runtime not available. Install with: pip install qiskit-ibm-runtime"
                         {:error (str e)})))))))

(defn list-ibm-backends
  "List available IBM Quantum backends.

   Args:
   - service: IBM Quantum service (optional, creates new if not provided)
   - operational-only: Only show operational backends (default: true)

   Returns: Vector of backend names"
  ([]
   (list-ibm-backends nil true))
  ([service]
   (list-ibm-backends service true))
  ([service operational-only]
   (let [svc (or service (ibm-quantum-service))]
     (core/->clj (py/call-attr svc "backends" :operational operational-only)))))

(defn get-ibm-backend
  "Get a specific IBM Quantum backend.

   Args:
   - service: IBM Quantum service
   - backend-name: Name of the backend

   Returns: Backend object"
  [service backend-name]
  (py/call-attr service "backend" backend-name))

;; Backend properties and information
(defn backend-name
  "Get the name of a backend."
  [backend]
  (py/get-attr backend "name"))

(defn backend-version
  "Get the version of a backend."
  [backend]
  (py/get-attr backend "version"))

(defn backend-status
  "Get the status of a backend."
  [backend]
  (core/->clj (py/call-attr backend "status")))

(defn backend-configuration
  "Get the configuration of a backend."
  [backend]
  (core/->clj (py/call-attr backend "configuration")))

(defn backend-properties
  "Get the properties of a backend (for real hardware)."
  [backend]
  (try
    (core/->clj (py/call-attr backend "properties"))
    (catch Exception _ nil)))

(defn num-qubits
  "Get the number of qubits available on a backend."
  [backend]
  (let [config (backend-configuration backend)]
    (get config "n_qubits")))

(defn coupling-map
  "Get the coupling map (connectivity) of a backend."
  [backend]
  (let [config (backend-configuration backend)]
    (get config "coupling_map")))

(defn basis-gates
  "Get the basis gates supported by a backend."
  [backend]
  (let [config (backend-configuration backend)]
    (get config "basis_gates")))

(defn max-shots
  "Get the maximum number of shots for a backend."
  [backend]
  (let [config (backend-configuration backend)]
    (get config "max_shots")))

(defn simulator?
  "Check if a backend is a simulator."
  [backend]
  (let [config (backend-configuration backend)]
    (get config "simulator" false)))

;; Transpilation
(defn transpile
  "Transpile circuits for a specific backend.

   Args:
   - circuits: Single circuit or collection of circuits
   - backend: Target backend
   - options: Transpilation options

   Options:
   - :optimization-level - Optimization level (0-3)
   - :initial-layout - Initial qubit layout
   - :seed-transpiler - Random seed for transpiler

   Returns: Transpiled circuit(s)"
  ([circuits backend]
   (transpile circuits backend {}))
  ([circuits backend {:keys [optimization-level initial-layout seed-transpiler]}]
   (core/with-python-context
     (core/with-qiskit
       (try
         (let [qk (py/import-module "qiskit")
               transpile-fn (py/get-attr qk "transpile")
               ;; Explicitly convert to Python list using py/->py-list
               circuits-arg (if (coll? circuits)
                              (py/->py-list circuits)
                              (py/->py-list [circuits]))
               single-circuit? (not (coll? circuits))]
           (let [result (if (or optimization-level initial-layout seed-transpiler)
                          (transpile-fn circuits-arg
                                       :backend backend
                                       :optimization_level optimization-level
                                       :initial_layout (when initial-layout (core/->py initial-layout))
                                       :seed_transpiler seed-transpiler)
                          (transpile-fn circuits-arg :backend backend))]
             (if single-circuit?
               (first result)
               (core/->clj result))))
         (catch Exception e
           (throw (ex-info "Circuit transpilation failed"
                           {:circuits (if (coll? circuits) (count circuits) 1)
                            :backend (str backend)
                            :options {:optimization-level optimization-level
                                     :initial-layout initial-layout
                                     :seed-transpiler seed-transpiler}
                            :error (str e)}
                           e))))))))

(defn transpile-with-pass-manager
  "Transpile using a custom pass manager.

   Args:
   - circuits: Circuits to transpile
   - pass-manager: PassManager object

   Returns: Transpiled circuits"
  [circuits pass-manager]
  (py/call-attr pass-manager "run" circuits))

(defn create-pass-manager
  "Create a custom transpilation pass manager.

   Args:
   - passes: Collection of transpiler passes
   - backend: Target backend (optional)

   Returns: PassManager object"
  ([passes]
   (create-pass-manager passes nil))
  ([passes backend]
   (core/with-qiskit
     (let [transpiler (py/import-module "qiskit.transpiler")
           pm (py/call-attr transpiler "PassManager")]
       (doseq [pass passes]
         (py/call-attr pm "append" pass))
       pm))))

;; Circuit execution
(defn run-circuit
  "Run a circuit on a backend.

   Args:
   - backend: Backend to run on
   - circuits: Single circuit or collection of circuits
   - shots: Number of shots (optional)
   - memory: Return memory (individual shot results) (optional)

   Returns: Job object"
  ([backend circuits]
   (run-circuit backend circuits nil nil))
  ([backend circuits shots]
   (run-circuit backend circuits shots nil))
  ([backend circuits shots memory]
   (core/with-python-context
     (try
       (let [circuits-list (if (coll? circuits) circuits [circuits])
             kwargs (cond-> {}
                      shots (assoc :shots shots)
                      memory (assoc :memory memory))]
         (if (empty? kwargs)
           (py/call-attr backend "run" circuits-list)
           (py/call-attr-kw backend "run" [circuits-list] kwargs)))
       (catch Exception e
         (throw (ex-info "Circuit execution failed"
                         {:backend (str backend)
                          :circuits (if (coll? circuits) (count circuits) 1)
                          :shots shots
                          :memory memory
                          :error (str e)}
                         e)))))))

(defn job-status
  "Get the status of a job."
  [job]
  (core/->clj (py/call-attr job "status")))

(defn job-result
  "Get the result of a completed job.

   Args:
   - job: Job object
   - timeout: Timeout in seconds (optional)

   Returns: Result object"
  ([job]
   (core/with-python-context
     (try
       (py/call-attr job "result")
       (catch Exception e
         (throw (ex-info "Job result retrieval failed"
                         {:job (str job)
                          :error (str e)}
                         e))))))
  ([job timeout]
   (core/with-python-context
     (try
       (if timeout
         (py/call-attr-kw job "result" [] {:timeout timeout})
         (py/call-attr job "result"))
       (catch Exception e
         (throw (ex-info "Job result retrieval failed"
                         {:job (str job)
                          :timeout timeout
                          :error (str e)}
                         e)))))))

(defn job-cancel
  "Cancel a running job."
  [job]
  (py/call-attr job "cancel"))

(defn wait-for-job
  "Wait for a job to complete.

   Args:
   - job: Job object
   - timeout: Maximum wait time in seconds (optional)

   Returns: Result object when complete"
  ([job]
   (wait-for-job job nil))
  ([job timeout]
   (job-result job timeout)))

;; Result processing
(defn get-counts
  "Get measurement counts from a result.

   Args:
   - result: Result object
   - circuit-index: Index of circuit (default: 0)

   Returns: Map of measurement outcomes to counts"
  ([result]
   (get-counts result 0))
  ([result circuit-index]
   (core/->clj (py/call-attr result "get_counts" circuit-index))))

(defn get-memory
  "Get individual shot results (memory) from a result.

   Args:
   - result: Result object
   - circuit-index: Index of circuit (default: 0)

   Returns: Vector of measurement outcomes"
  ([result]
   (get-memory result 0))
  ([result circuit-index]
   (core/->clj (py/call-attr result "get_memory" circuit-index))))

(defn get-statevector
  "Get the final statevector from a statevector simulation.

   Args:
   - result: Result object
   - circuit-index: Index of circuit (default: 0)

   Returns: Statevector"
  ([result]
   (get-statevector result 0))
  ([result circuit-index]
   (py/call-attr result "get_statevector" circuit-index)))

(defn get-unitary
  "Get the unitary matrix from a unitary simulation.

   Args:
   - result: Result object
   - circuit-index: Index of circuit (default: 0)

   Returns: Unitary matrix"
  ([result]
   (get-unitary result 0))
  ([result circuit-index]
   (py/call-attr result "get_unitary" circuit-index)))

;; Noise modeling (requires Aer)
(defn noise-model
  "Create a noise model.

   Returns: NoiseModel object"
  []
  (core/with-qiskit
    (try
      (let [noise (py/import-module "qiskit_aer.noise")]
        (py/call-attr noise "NoiseModel"))
      (catch Exception e
        (throw (ex-info "Qiskit Aer not available for noise modeling"
                        {:error (str e)}))))))

(defn add-quantum-error
  "Add quantum error to a noise model.

   Args:
   - noise-model: NoiseModel object
   - error: QuantumError object
   - instructions: Instructions to apply error to
   - qubits: Qubits to apply error to (optional)

   Returns: Modified noise model"
  ([noise-model error instructions]
   (add-quantum-error noise-model error instructions nil))
  ([noise-model error instructions qubits]
   (if qubits
     (py/call-attr noise-model "add_quantum_error" error instructions qubits)
     (py/call-attr noise-model "add_quantum_error" error instructions))
   noise-model))

(defn depolarizing-error
  "Create a depolarizing error.

   Args:
   - prob: Error probability
   - num-qubits: Number of qubits

   Returns: QuantumError object"
  [prob num-qubits]
  (core/with-qiskit
    (try
      (let [noise (py/import-module "qiskit_aer.noise")]
        (py/call-attr noise "depolarizing_error" prob num-qubits))
      (catch Exception e
        (throw (ex-info "Qiskit Aer not available for noise modeling"
                        {:error (str e)}))))))

;; High-level execution functions
(defn execute-circuit
  "High-level function to execute a circuit and get results.

   Args:
   - circuit: Circuit to execute
   - backend: Backend to use (optional, defaults to Aer simulator)
   - shots: Number of shots (default: 1024)
   - transpile?: Whether to transpile (default: true)

   Returns: Map of measurement outcomes to counts"
  ([circuit]
   (execute-circuit circuit nil 1024 true))
  ([circuit backend]
   (execute-circuit circuit backend 1024 true))
  ([circuit backend shots]
   (execute-circuit circuit backend shots true))
  ([circuit backend shots transpile?]
   (let [backend (or backend (aer-simulator))
         circuit (if transpile? (transpile circuit backend) circuit)
         job (run-circuit backend circuit shots)
         result (wait-for-job job)]
     (get-counts result))))

(defn execute-statevector
  "Execute a circuit and return the final statevector.

   Args:
   - circuit: Circuit to execute
   - backend: Statevector backend (optional)

   Returns: Final statevector"
  ([circuit]
   (execute-statevector circuit nil))
  ([circuit backend]
   (let [backend (or backend (aer-simulator {:method :statevector}))
         job (run-circuit backend circuit)
         result (wait-for-job job)]
     (get-statevector result))))

(comment
  ;; Example usage
  (require '[com.justinwoodring.qiskit-clj.core :as core])
  (require '[com.justinwoodring.qiskit-clj.circuit :as circuit])
  (require '[com.justinwoodring.qiskit-clj.gates :as gates])
  (core/initialize!)

  ;; Basic simulation
  (let [bell-circuit (-> (circuit/quantum-circuit 2)
                         (gates/hadamard 0)
                         (gates/cnot 0 1)
                         (circuit/measure-all))
        counts (execute-circuit bell-circuit)]
    (println "Bell state counts:" counts))

  ;; Using different backends
  (let [backend (aer-simulator {:method :statevector})
        circuit (-> (circuit/quantum-circuit 2)
                    (gates/hadamard 0)
                    (gates/cnot 0 1))
        statevector (execute-statevector circuit backend)]
    (println "Final statevector:" statevector))

  ;; IBM Quantum (requires token)
  #_(let [service (ibm-quantum-service "your-token")
        backends (list-ibm-backends service)
        backend (get-ibm-backend service "ibm_brisbane")]
    (println "Available backends:" backends)
    (println "Backend info:" (backend-configuration backend))))