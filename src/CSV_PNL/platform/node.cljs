(ns CSV-PNL.platform.node
  (:require
   [cljs.nodejs :as nodejs]
   [cljs.core.async :refer [go <! chan put!] :as async]))

(def fs (nodejs/require "fs"))

(defn slurp [file-path]
  (let [c (chan)]
    (.readFile
     ^js fs file-path
     (fn [err data]
       (if err
         (put! c {:error err})
         (put! c {:content (.toString ^js data)}))))
    c))


(comment 
  (go
    (let [result (<! (slurp "resources/transaction_data.csv"))] 
      (println result)))
  )