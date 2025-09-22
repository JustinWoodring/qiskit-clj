(ns com.justinwoodring.qiskit-clj.minimal-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.justinwoodring.qiskit-clj.core :as core]))

(deftest basic-initialization-test
  (testing "Python initialization without Qiskit imports"
    (is (nil? (core/initialize!)))
    (is (true? core/*python-initialized*))))

(deftest data-conversion-test-minimal
  (testing "Basic data conversion"
    (core/ensure-initialized!)
    (let [simple-data [1 2 3]
          converted (core/->py simple-data)
          back (core/->clj converted)]
      (is (= simple-data back)))))