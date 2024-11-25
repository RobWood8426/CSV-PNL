(ns CSV-PNL.util.core
  (:require
   [clojure.core.async :refer [<! go]]
   [clojure.string :as string]
   [CSV-PNL.io :as io]))

(def column-config
  {:amount
   {:key :Amount
    :transform js/Number}
   :date
   {:key :Date
    :transform
    (fn [date-str]
      (let [date (js/Date. date-str)]
        (if (js/isNaN (. date getTime))
          (throw (js/Error. (str "Invalid date format: " date-str)))
          date)))}
   :type
   {:key :Type
    :transform identity}
   :description
   {:key :Description
    :transform identity}})

(defn parse-line [line]
  (string/split line #","))

(defn validate-headers [headers]
  (let [required-headers (set (map (comp name :key) (vals column-config)))
        actual-headers (set headers)]
    {:valid? (every? actual-headers required-headers)
     :missing (seq (remove actual-headers required-headers))
     :has-headers? (some required-headers actual-headers)}))

(defn transform-value [{:keys [key transform]} value]
  (try
    (if (= key :Amount)
      (-> value transform (.toFixed 2) js/Number)
      (transform value))
    (catch :default e
      (throw (js/Error. (str "Error processing column " (name key) ": " (.-message e)))))))

(defn process-row [headers row]
  (let [mapped (zipmap headers row)]
    (reduce-kv
     (fn [acc config-key config]
       (if-let [value (get mapped (keyword (string/lower-case (name (:key config)))))]
         (assoc acc config-key (transform-value config value))
         acc))
     {}
     column-config)))

(defn process-rows [headers rows]
  (->>
   rows
   (map second)
   (map (partial process-row headers))))

(defn parse-csv [csv-string]
  (let [lines (string/split-lines csv-string)
        first-line (parse-line (first lines))
        {:keys [valid? missing has-headers?]} (validate-headers first-line)]
    (cond
      (not has-headers?)
      {:error "CSV appears to be missing a header row (Or hasn't mapped headers correctly)"
       :expected-headers (map (comp name :key) (vals column-config))}

      (not valid?)
      {:error (str "Required columns are missing: " (string/join ", " missing))
       :missing missing}

      :else
      (let [header-mapping
            (reduce-kv
             (fn [m _ {:keys [key]}]
               (assoc m (name key) (keyword (string/lower-case (name key)))))
             {}
             column-config)
            headers (map #(get header-mapping % %) first-line)
            expected-column-count (count headers)
            rows (map-indexed vector (map parse-line (rest lines)))
            invalid-rows
            (filter
             (fn [[_idx row]]
               (not= (count row) expected-column-count))
             rows)]
        (cond
          (seq invalid-rows)
          {:error "Some rows have incorrect number of columns"
           :invalid-rows
           (map
            (fn [[idx row]]
              {:line-number (+ idx 2) ; +2 for 0-based index and header row
               :expected expected-column-count
               :got (count row)
               :row row})
            invalid-rows)}

          :else
          {:success true
           :data (process-rows headers rows)})))))

(defn calculate-totals [csv-result]
  (if (:success csv-result)
    (let [{:keys [income expense]} 
          (reduce
           (fn [acc {:keys [amount type]}]
             (case type
               "Income" (update acc :income + amount)
               "Expense" (update acc :expense + amount)
               acc))
           {:income 0 :expense 0}
           (:data csv-result))]
      {:income (-> income (.toFixed 2) js/Number)
       :expense (-> expense (.toFixed 2) js/Number)})
    {:error (:error csv-result)}))

(comment


  (go
    (let [file (<! (io/read-file "resources/transaction_data.csv"))]
      (println file)))




  (calculate-totals
   (parse-csv
    (io/read-file "resources/transaction_data.csv")))

  ;; Valid CSV
  (parse-csv "Amount,Date,Type,Description\n100,2024-03-20,income,Salary")

;; Missing headers
  (parse-csv "100,2024-03-20,income,Salary")

;; Missing required columns
  (parse-csv "Amount,Date,Description\n100,2024-03-20,Salary")


  (parse-csv "Amount,Date,Type,Description\n100,2024-03-20,income,Salary\n101"))