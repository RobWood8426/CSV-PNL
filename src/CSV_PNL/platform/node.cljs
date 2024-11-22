(ns CSV-PNL.platform.node
  (:require [cljs.nodejs :as nodejs]))

(def fs (nodejs/require "fs"))

(defn node-slurp [file-path]
  (let [content (.toString ^js (.readFileSync ^js fs file-path))]
    content))