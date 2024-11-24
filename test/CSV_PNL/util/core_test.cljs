(ns CSV-PNL.util.core-test
  (:require
   [cljs.test :refer-macros [deftest testing is run-tests]]
   [CSV-PNL.util.core :as core]))

(deftest util-core-tests
  (testing "Valid CSV parsing"
    (let [result (core/parse-csv "Amount,Date,Type,Description\n100,2024-03-20,income,Salary")]
      (is (:success result))
      (is (= [{:amount 100
               :date #inst "2024-03-20T00:00:00.000Z"
               :type "income"
               :description "Salary"}]
             (:data result)))))

  (testing "Missing headers"
    (let [result (core/parse-csv "100,2024-03-20,income,Salary")]
      (is (:error result))))

  (testing "Missing required columns"
    (let [result (core/parse-csv "Amount,Date,Description\n100,2024-03-20,Salary")]
      (is (:error result))
      (is (= ["Type"] (:missing result)))))

  (testing "Invalid row count"
    (let [result (core/parse-csv "Amount,Date,Type,Description\n100,2024-03-20,income,Salary\n101")]
      (is (:error result))
      (is (= [{:line-number 3 
               :expected 4
               :got 1
               :row ["101"]}]
             (:invalid-rows result))))))

(comment
  (run-tests)
  )