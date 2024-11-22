(ns CSV-PNL.platform.browser
  (:require [cljs.core.async :refer [chan put! close!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.core.async :refer-macros [go]]))

(defn browser-slurp [file]
  (let [out (chan)]
    (go
      (try
        (let [reader (js/FileReader.)
              promise
              (js/Promise.
               (fn [resolve reject]
                 (set!
                  (.-onload reader)
                  (fn [e]
                    (resolve (.. e -target -result))))
                 (set!
                  (.-onerror reader)
                  (fn [e]
                    (reject (.. e -target -error))))
                 (.. reader (readAsText file))))]
          (put! out (<p! promise))
          (close! out))
        (catch js/Error e
          (put! out e)
          (close! out))))
    out))