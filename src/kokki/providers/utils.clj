(ns kokki.providers.utils 
  (:require
    [babashka.fs :as fs]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

(defonce done-fn
  (constantly :done))

(defn get-list-of-hooks-from-directory
  "given a directory location in local system, return the list of hooks"
  [directory-location]
  (let [directory-location  (fs/path directory-location)]
    (let [list-hooks        (fs/list-dir directory-location)]
      (->> list-hooks
           (mapv #(hash-map :name (fs/file-name %)
                            :location %))))))

(defn ->hooks-filter-fn
  "given the hook-name, returns a function which filters by hook's name"
  [hook-name]
  (if (some? hook-name)
    (partial filterv #(str/includes? (:name %) hook-name))    
    (constantly true)))

(def get-src-hooks-dir
  "returns the src hooks dir"
  :src)

(def get-target-hooks-dir
  "returns the target hooks dir"
  :target)

(defn setup-hooks
  "copy the hooks to your local setup"
  [hooks-dir
   {:keys [location
           content
           name
           error]}]
  (fs/create-dirs (get-target-hooks-dir hooks-dir))
  (if (some? error)
    (log/warnf "problem with setting up hook: %s failed with : %s" name error)
    (let [hook-path (fs/path (get-target-hooks-dir hooks-dir) name)]
      (if (some? location)
        (fs/copy location hook-path)
        (fs/write-bytes hook-path (.getBytes (str content))))
      (fs/set-posix-file-permissions hook-path "rwxr-xr-x"))))
