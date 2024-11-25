(ns CSV-PNL.util.core
  (:require
   [clojure.core.async :refer [<! go]]
   [clojure.string :as string]))

(def default-column-config
  {:amount
   {:key :Amount
    :required? true
    :transform js/Number}
   :date
   {:key :Date
    :required? true
    :transform
    (fn [date-str]
      (let [date (js/Date. date-str)]
        (if (js/isNaN (. date getTime))
          (throw (js/Error. (str "Invalid date format: " date-str)))
          date)))}
   :type
   {:key :Type
    :required? true
    :transform identity}
   :description
   {:key :Description
    :required? true
    :transform identity}
   :category
   {:key :Category
    :required? false
    :transform identity}})

(defn parse-line [line]
  (string/split line #","))

(defn validate-headers [{:keys [title-mapping column-config]} headers]
  (let [merged-config (merge default-column-config column-config)

        required-columns
        (set
         (map
          (comp name :key)
          (filter
           :required?
           (vals merged-config))))

        all-headers (set (map (comp name :key) (vals merged-config)))
        mapped-headers (map #(get title-mapping (keyword %) %) headers)
        actual-headers (set mapped-headers)]
    {:valid? (every? actual-headers required-columns)
     :missing (seq (remove actual-headers required-columns))
     :has-headers? (some all-headers actual-headers)
     :mapped-headers mapped-headers}))

(comment
  (validate-headers 
   {:title-mapping {:SomeCrazyDescription "Description"}} 
   ["Amount" "Date" "SomeCrazyDescription" "Type" "Category"])
  
  (validate-headers {} ["Amount" "Date" "Description" "Type" "Category"]))

(defn transform-value [{:keys [key transform]} value]
  (try
    (if (= key :Amount)
      (-> value transform (.toFixed 2) js/Number)
      (transform value))
    (catch :default e
      (throw (js/Error. (str "Error processing column " (name key) ": " (.-message e)))))))

(defn process-row [{:keys [column-config]} headers row]
  (let [mapped (zipmap headers row)]
    (reduce-kv
     (fn [acc config-key config]
       (if-let [value (get mapped (name (:key config)))]
         (assoc acc config-key (transform-value config value))
         acc))
     {}
     (merge
      default-column-config
      column-config))))

(defn process-rows [opts headers rows]
  (->>
   rows
   (map second)
   (map (partial process-row opts headers))))

(comment

  (require '[CSV-PNL.platform.node :as node])

  (go
    (let [file (<! (node/slurp "resources/transaction_data.csv"))]
      (println
       (parse-csv (:content file))))))

(defn parse-csv
  ([csv-string]
   (parse-csv csv-string {}))
  ([csv-string {:keys [column-config] :as opts}] 
   (let [lines (string/split-lines csv-string)
         first-line (parse-line (first lines))
         {:keys [valid? missing has-headers? mapped-headers]} (validate-headers opts first-line)]

     (cond
       (not has-headers?)
       {:error "CSV appears to be missing a header row (Or hasn't mapped headers correctly)"
        :expected-headers (map (comp name :key) (vals (merge default-column-config column-config)))}

       (not valid?)
       {:error (str "Required columns are missing: " (string/join ", " missing))
        :missing missing}

       :else
       (let [headers mapped-headers
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
            :data (process-rows opts headers rows)}))))))

(defn calculate-totals [csv-result]
  (if (:success csv-result)
    (let [{:keys [income expense categories]}
          (reduce
           (fn [acc {:keys [amount type category] :as transaction}]
             (let [cat (or category "Uncategorized")
                   updated-cat
                   (update-in
                    acc [:categories cat]
                    (fnil
                     (fn [cat-data]
                       (-> cat-data
                           (update (keyword (string/lower-case type)) + amount)
                           (update :transactions conj transaction)))
                     {:income 0 :expense 0 :transactions []}))]
               (case type
                 "Income" (update updated-cat :income + amount)
                 "Expense" (update updated-cat :expense + amount)
                 updated-cat)))
           {:income 0 :expense 0 :categories {}}
           (:data csv-result))]
      {:income (-> income (.toFixed 2) js/Number)
       :expense (-> expense (.toFixed 2) js/Number)
       :categories
       (update-vals
        categories
        #(update-vals
          %
          (fn [v]
            (if (number? v)
              (-> v (.toFixed 2) js/Number)
              v))))})
    {:error (:error csv-result)}))

(comment

  (go (println (<! (CSV-PNL.platform.node/slurp "resources/transaction_data.csv"))))

  (go
    (println
     (calculate-totals
      (parse-csv
       (:content
        (<! (CSV-PNL.platform.node/slurp "resources/transaction_data.csv")))
       {}))))

  ;; Valid CSV
  (calculate-totals
   (parse-csv "Amount,Date,Type,Description\n100,2024-03-20,income,Salary" {}))

;; Missing headers
  (parse-csv "100,2024-03-20,income,Salary" {})

;; Missing required columns
  (parse-csv "Amount,Date,Description\n100,2024-03-20,Salary" {})

  (parse-csv "Amount,Date,Type,Description\n100,2024-03-20,income,Salary\n101" {}))