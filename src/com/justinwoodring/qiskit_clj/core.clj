(ns com.justinwoodring.qiskit-clj.core
  "Core utilities for Qiskit-Clojure integration.

   This namespace provides the foundation for working with Qiskit from Clojure,
   including Python environment initialization, data conversion utilities,
   and error handling."
  (:require [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]
            [clojure.string :as str]))

(def ^:dynamic *python-initialized* false)

(defn initialize!
  "Initialize the Python environment and import Qiskit.

   Options:
   - :python-executable - Path to Python executable (optional)
   - :python-library-path - Path to Python library (optional)

   Example:
   (initialize!)
   (initialize! {:python-executable \"/opt/conda/bin/python\"})"
  ([] (initialize! {}))
  ([{:keys [python-executable python-library-path]}]
   (when-not *python-initialized*
     (try
       (py/initialize! (cond-> {:python-library :system
                                :python-home-directory nil}
                         python-executable (assoc :python-executable python-executable)
                         python-library-path (assoc :python-library-path python-library-path)))
       (alter-var-root #'*python-initialized* (constantly true))
       (println "Python environment initialized for Qiskit-Clojure")
       (println "WARNING: JVM may crash during shutdown due to libpython-clj2/Python 3.13 compatibility issues")
       (catch Exception e
         (throw (ex-info "Failed to initialize Python environment"
                         {:error (str e)
                          :recommendation "Try installing Python 3.11 or 3.12"}
                         e))))

     ;; Add a shutdown hook to try to prevent crashes
     (.addShutdownHook
      (Runtime/getRuntime)
      (Thread.
       (fn []
         (try
           (println "Attempting graceful Python shutdown...")
           ;; Don't call py/finalize! as it may cause crashes
           (catch Exception e
             (println "Warning: Error during Python cleanup:" (str e))))))))))

(defn ensure-initialized!
  "Ensure Python environment is initialized, initialize if not."
  []
  (when-not *python-initialized*
    (initialize!)))

(defmacro with-qiskit
  "Execute body with Qiskit environment initialized and proper error handling."
  [& body]
  `(do
     (ensure-initialized!)
     (try
       ~@body
       (catch Exception e#
         (if (qiskit-error? e#)
           (throw (ex-info "Qiskit operation failed"
                           {:type :qiskit-error
                            :original-error (str e#)}
                           e#))
           (throw e#))))))

(defmacro with-python-context
  "Execute body with safer Python context management."
  [& body]
  `(try
     ~@body
     (catch Exception e#
       (when (str/includes? (str e#) "SIGSEGV")
         (throw (ex-info "Critical Python/JVM interaction error detected"
                         {:type :jvm-crash-risk
                          :recommendation "This operation may cause JVM instability"
                          :error (str e#)}
                         e#)))
       (throw e#))))

(defn ->clj
  "Convert Python object to Clojure data structure.

   This is a wrapper around py/->jvm with additional handling
   for common Qiskit types and safer error handling."
  [py-obj]
  (try
    (when py-obj
      (py/->jvm py-obj))
    (catch Exception e
      (throw (ex-info "Failed to convert Python object to Clojure"
                      {:python-object (str py-obj)
                       :error (str e)}
                      e)))))

(defn ->py
  "Convert Clojure data structure to Python object.

   This is a wrapper around py/->python with additional handling
   for common Clojure types used in quantum computing and safer error handling."
  [clj-obj]
  (try
    (py/->python clj-obj)
    (catch Exception e
      (throw (ex-info "Failed to convert Clojure object to Python"
                      {:clojure-object clj-obj
                       :error (str e)}
                      e)))))

(defn py-call
  "Call a Python function with Clojure arguments.

   Arguments are automatically converted to Python types,
   and the result is converted back to Clojure types."
  [py-fn & args]
  (try
    (-> (apply py-fn (map ->py args))
        ->clj)
    (catch Exception e
      (throw (ex-info "Python function call failed"
                      {:function (str py-fn)
                       :args args
                       :error (str e)}
                      e)))))

(defn py-get-attr
  "Get an attribute from a Python object, converting to Clojure."
  [py-obj attr-name]
  (try
    (-> (py/get-attr py-obj attr-name)
        ->clj)
    (catch Exception e
      (throw (ex-info "Failed to get Python attribute"
                      {:object (str py-obj)
                       :attribute attr-name
                       :error (str e)}
                      e)))))

(defn py-call-attr
  "Call a method on a Python object with Clojure arguments."
  [py-obj method-name & args]
  (try
    (-> (apply py/call-attr py-obj method-name (map ->py args))
        ->clj)
    (catch Exception e
      (throw (ex-info "Python method call failed"
                      {:object (str py-obj)
                       :method method-name
                       :args args
                       :error (str e)}
                      e)))))

(defn qiskit-error?
  "Check if an exception is a Qiskit-related error."
  [ex]
  (and (instance? Exception ex)
       (str/includes? (str ex) "qiskit")))

(defmacro with-qiskit-error-handling
  "Execute body with Qiskit-specific error handling."
  [& body]
  `(try
     ~@body
     (catch Exception e#
       (if (qiskit-error? e#)
         (throw (ex-info "Qiskit error occurred"
                         {:type :qiskit-error
                          :original-error (str e#)}
                         e#))
         (throw e#)))))

(defn qiskit-version
  "Get the version of Qiskit being used."
  []
  (with-qiskit
    (require-python 'qiskit)
    (py-get-attr (py/import-module "qiskit") "__version__")))

(defn list-available-backends
  "List all available Qiskit backends."
  []
  (with-qiskit
    (require-python '[qiskit :as qk])
    (let [aer-provider (try
                         (require-python '[qiskit_aer :as aer])
                         (py-call-attr (py/import-module "qiskit_aer") "AerProvider")
                         (catch Exception _ nil))]
      (cond-> []
        aer-provider (into (map str (py-call-attr aer-provider "backends")))))))

(defn validate-qubit-count
  "Validate that qubit count is positive integer."
  [n]
  (when-not (and (integer? n) (pos? n))
    (throw (ex-info "Qubit count must be a positive integer"
                    {:provided n :type :invalid-qubit-count})))
  n)

(defn validate-qubit-index
  "Validate that qubit index is within range [0, max-qubits)."
  [idx max-qubits]
  (when-not (and (integer? idx) (<= 0 idx (dec max-qubits)))
    (throw (ex-info "Qubit index out of range"
                    {:index idx :max-qubits max-qubits :type :invalid-qubit-index})))
  idx)

(defn format-complex
  "Format a complex number for display."
  [z]
  (if (number? z)
    (str z)  ; Simple string representation for real numbers
    (str z)))  ; For other types, just convert to string

(comment
  ;; Example usage
  (initialize!)
  (qiskit-version)
  (list-available-backends)

  ;; Error handling example
  (with-qiskit-error-handling
    (qiskit-version)))