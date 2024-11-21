(ns CSV-PNL.core
  (:require
   [cljs.nodejs :as nodejs] 
   [clojure.string :as string]))

(println "Starting Point")

(defn ^export create-pnl [csv] 
  (println "CSV Uploaded"))


(defn parse-line [line]
  (string/split line #","))

(defn parse-csv [csv-string]
  (let [lines (string/split-lines csv-string)
        headers (map keyword (parse-line (first lines)))
        rows (map parse-line (rest lines))]
    (map #(zipmap headers %) rows)))


(def fs (nodejs/require "fs"))

(defn node-slurp [file-path]
  (let [content (.toString (.readFileSync fs file-path))]
    content
    ))



(comment
  (parse-csv
   (node-slurp "resources/transaction_data.csv"))
  
  )