(ns CSV-PNL.platform.common
  (:require
   [CSV-PNL.platform.node :as node]
   [CSV-PNL.platform.browser :as browser]))

(def env
  (cond
    (exists? js/process)
    :node

    (and
     (exists? js/window)
     (exists? js/document))
    :browser))

(defn read-file [file-path]
  (case env
    :node   (node/node-slurp file-path)
    :browser (browser/browser-slurp file-path)))

(comment
  env)


