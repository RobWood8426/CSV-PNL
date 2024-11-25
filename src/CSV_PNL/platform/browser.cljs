(ns CSV-PNL.platform.browser
  (:require [cljs.core.async.interop :refer-macros [<p!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn slurp [file]
  (go
    (let [result
          (<p!
           (js/Promise.
            (fn [resolve reject]
              (let [reader (js/FileReader.)]
                (.addEventListener reader "load" #(resolve (.. % -target -result)))
                (.addEventListener reader "error" #(reject (.. % -target -error)))
                (.readAsText reader file)))))]
      result)))

(comment
  (go
    (let [result (<p! (slurp "resources/transaction_data.csv"))]
      (println result))))