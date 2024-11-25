(ns CSV-PNL.platform.node
  (:require
   [cljs.nodejs :as nodejs]
   [cljs.core.async :refer [go <! chan put!] :as async]))

(def fs (nodejs/require "fs"))

(defn slurp [input]
  (let [c (chan)]
    (cond
      ; Case 1: input is a string path to a file
      (and (string? input) (js-invoke ^js fs "existsSync" input))
      (js-invoke
       ^js fs "readFile"
       input
       (fn [err data]
         (if err
           (put! c {:error err})
           (put! c {:content (.toString data)}))))

      ; Case 2: input is a file-like object with path
      (and (object? input) (or (.-path input) (.-filename input)))
      (js-invoke
       ^js fs "readFile"
       (or (.-path input) (.-filename input))
       (fn [err data]
         (if err
           (put! c {:error err})
           (put! c {:content (.toString data)}))))

      ; Case 3: input is already a string content
      (string? input)
      (put! c {:content input})

      ; Default: invalid input
      :else
      (put! c {:error (js/Error. "Invalid input: must be a file path, File object, or string content")}))
    c))

(comment
  ; With file path
  (go

    (println
     (<! (slurp "resources/transaction_data.csv"))))

  ; With raw string
  (go (println (<! (slurp "col1,col2\n1,2"))))

  ; With Node.js file object (e.g., from express-fileupload or similar)
  (go (println (<! (slurp #js{:filename "resources/transaction_data.csv"})))))