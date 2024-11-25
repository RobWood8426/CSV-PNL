(ns CSV-PNL.platform.common
  (:require
   #?@(:node
       [[CSV-PNL.platform.node :as dist]]
       :cljs
       [[CSV-PNL.platform.browser :as dist]])))

(defn read-file [file-path]
  #?(:clj (slurp file-path)
     :default (dist/slurp file-path)))


