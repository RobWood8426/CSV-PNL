(ns CSV-PNL.core
  (:require
   [CSV-PNL.io :as io]
   [clojure.string :as string]))

(println "Starting Point")

(defn parse-line [line]
  (string/split line #","))

(defn parse-csv [csv-string]
  (let [lines (string/split-lines csv-string)
        headers (map keyword (parse-line (first lines)))
        rows (map parse-line (rest lines))]
    (map #(zipmap headers %) rows)))

(defn calculate-totals [transactions]
  (reduce (fn [acc {:keys [amount type]}]
            (let [amount-num (js/parseFloat amount)]
              (case type
                "income" (update acc :income + amount-num)
                "expense" (update acc :expense + amount-num)
                acc)))
          {:income 0 :expense 0}
          transactions))

(defn ^export create-pnl [csv]
  (let [content (io/read-file csv)
        transactions (parse-csv content)
        totals (calculate-totals transactions)]
    (assoc totals 
           :net-profit (- (:income totals) 
                         (:expense totals))
           :transactions transactions)))




