(ns CSV-PNL.platform.browser-test
  (:require
   [cljs.core.async :refer [<! go]]
   [cljs.test :refer-macros [deftest is async] :refer [run-tests]]
   [CSV-PNL.platform.browser :as browser]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (println "end-run-tests" m)
  (if (cljs.test/successful? m)
    (println "Success!")
    (println "FAIL")))

(deftest slurp-string-test
  (async done
    (go
      (let [input "col1,col2\n1,2"
            result (<! (browser/slurp input))]
        (is (= (:content result) input))
        (done)))))

(deftest slurp-file-test
  (async done
    (go
      (let [input (js/File. #js["some,csv\ndata"] "test.csv")
            result (<! (browser/slurp input))]
        (is (= (:content result) "some,csv\ndata"))
        (done)))))

(deftest slurp-blob-test
  (async done
    (go
      (let [input (js/Blob. #js["some,csv\ndata"] #js{:type "text/csv"})
            result (<! (browser/slurp input))]
        (assert (= (:content result) "some,csv\ndata1"))
        (done)))))

(deftest slurp-invalid-input-test
  (async done
    (go
      (let [result (<! (browser/slurp #js{}))]
        (is (:error result))
        (is (instance? js/Error (:error result)))
        (is (= (.-message (:error result))
               "Invalid input: must be a File object or string content"))
        (done)))))



(comment 

  (run-tests)
  )

