(ns kokki.providers.inline 
  (:require
    [kokki.providers.proto :as proto]
    [kokki.providers.utils :as utils]))

(defrecord InlineProvider [hooks-dir]
  proto/IProvider
  
  (get-token [_]
    nil)
  
  (get-hooks [_]
    (utils/get-list-of-hooks-from-directory (utils/get-src-hooks-dir hooks-dir)))
  
  (list-hooks [this]
    {:hooks (some->> (proto/get-hooks this)
                     (mapv :name)
                     not-empty)})
  
  (apply-hooks [this]
    (proto/apply-hooks this nil))
  
  (apply-hooks [this hook-name]
    (->> (proto/get-hooks this)
         (filterv (utils/->hooks-filter-fn hook-name))
         (mapv (partial utils/setup-hooks hooks-dir))
         utils/done-fn)))
