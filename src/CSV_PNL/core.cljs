(ns CSV-PNL.core
  (:require
   [CSV-PNL.io :as io]
   [CSV-PNL.util.core :as util]))

(println "Starting Point")

(defn ^export create-pnl [csv]
  (let [content (io/read-file csv)
        transactions (util/parse-csv content)
        totals (util/calculate-totals transactions)]
    (assoc totals
           :net-profit
           (- (:income totals)
              (:expense totals))
           :transactions transactions)))




