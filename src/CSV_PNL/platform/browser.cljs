(ns CSV-PNL.platform.browser
  (:require [cljs.core.async :refer [go <! chan put!] :as async]))

(defn file-like? [input]
  (or
   (instance? js/File input)
   (instance? js/Blob input)))

(defn read-file-async [file]
  (let [c (chan)
        reader (js/FileReader.)]
    (doto reader
      (-> .-onload (set! #(put! c {:content (.. % -target -result)})))
      (-> .-onerror (set! #(put! c {:error (js/Error. "Failed to read file")})))
      (.readAsText file))
    c))

(defn slurp
  "Reads content from a File/Blob object or string.
   Returns a channel that will receive a map with either:
   {:content string-content} or {:error error-object}"
  [input]
  (let [c (chan)]
    (cond
      (file-like? input)
      (async/pipe (read-file-async input) c)
      
      (string? input)
      (put! c {:content input})
      
      :else
      (put! c {:error (js/Error. "Invalid input: must be a File object or string content")}))
    c))

(comment
  ;; Example usage
  (go (println (<! (slurp "col1,col2\n1,2"))))
  (go (println (<! (slurp (js/File. #js["some,csv\ndata"] "test.csv")))))
  (go (println (<! (slurp (js/Blob. #js["some,csv\ndata"] #js{:type "text/csv"}))))))