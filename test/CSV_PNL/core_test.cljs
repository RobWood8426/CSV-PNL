(ns CSV-PNL.core-test
  (:require
   [cljs.test :refer-macros [deftest is run-all-tests]]
   [CSV-PNL.core :refer [createpnl]]))

(deftest create-pnl-test
  (is (= (createpnl "resources/transaction_data.csv")
         {:income 100 :expense 50 :net-profit 50 :transactions []})))

(comment

  (run-all-tests #"CSV-PNL.+"))