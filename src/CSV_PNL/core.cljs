(ns CSV-PNL.core
  (:require
   [CSV-PNL.io :as io]
   [cljs.core.async :refer [go <!]]
   [CSV-PNL.util.core :as util]))

(defn ^:export createpnl
  "Processes a CSV file to calculate profit and loss totals.
   
   Parameters:
   - csv: The CSV file to process (Platform dependant file object/path/blob etc)
   - callback: A JavaScript callback function that receives the calculated totals
   - opts: A map of options containing:
     - title-mapping: Optional Map of CSV column titles to standardized names (Date Type Description Amount Category)
     - column-config: Optional Configuration for how to process specific columns (Allows for fully overwriting default processing)
       see CSV-PNL.util.core/column-config for details
   The callback will receive a JavaScript object containing the calculated totals."
  [csv callback opts]
  (go
    (let [{:keys [content]} (<! (io/read-file csv))
          transactions (util/parse-csv content (js->clj opts {:keywordize-keys true}))
          totals (util/calculate-totals transactions)]
      (callback
       (clj->js totals)))))

(defn ^:export parsecsv 
  "Parses a CSV string into a sequence of transaction records.
   
   Parameters:
   - content: A string containing CSV data
   - opts: A map of options containing:
     - title-mapping: Optional Map of CSV column titles to standardized names (Date Type Description Amount Category)
     - column-config: Optional Configuration for how to process specific columns (Allows for fully overwriting default processing)
   
   Returns a sequence of maps, where each map represents a transaction with standardized keys."
  [content opts]
  (clj->js
   (util/parse-csv content opts)))






