(ns CSV-PNL.core
  (:require
   [CSV-PNL.io :as io]
   [cljs.core.async :refer [go <!]]
   [CSV-PNL.util.core :as util]))

(defn ^:export createpnl [csv callback {:keys [title-mapping column-config] :as opts}]
  (go
    (let [{:keys [content]} (<! (io/read-file csv))
          transactions (util/parse-csv content (js->clj opts {:keywordize-keys true}))
          totals (util/calculate-totals transactions)]
      (callback
       (clj->js totals)))))

(def ^:export parsecsv util/parse-csv)




