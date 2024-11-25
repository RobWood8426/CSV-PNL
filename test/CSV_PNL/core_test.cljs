(ns CSV-PNL.core-test
  (:require
   [cljs.test :refer-macros [deftest is run-all-tests]]
   [CSV-PNL.core :refer [createpnl]]))

(comment

  (run-all-tests #"CSV-PNL.+"))