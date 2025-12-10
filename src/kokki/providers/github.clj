(ns kokki.providers.github
  "i handle github provider setup"
  (:require [kokki.providers.proto :as proto]
            [kokki.providers.utils :as utils]
            [clojure.string :as str]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [babashka.fs :as fs]
            [babashka.process :as p])
  (:import [java.util Base64]))

;; =====utils-fn=====

(defn get-remote-hooks-location
  "returns the location of the remote hooks dir"
  [hooks-dir]
  (str "/contents/" (utils/get-src-hooks-dir hooks-dir)))

;; =====github API-fn(s)=====

(defn github-api-request
  "Make a request to GitHub API"
  [token repo path]
  (try
    (let [url (str "https://api.github.com/repos/" repo path)
          response (http/get url
                             {:headers {"Authorization" (str "token " token)
                                        "User-Agent" "kokki-hooks-manager"}
                              :as :json})]
      (if (= 200 (:status response))
        {:data (:body response)}
        {:error (str "GitHub API error: " (:status response))}))
    (catch Exception e
      (log/error "GitHub API request failed:" (Throwable->map e))
      {:error (.getMessage e)})))

(defn- download-hook-from-git
  "Download a hook file from GitHub repository"
  [token repo hooks-dir {hook-name :name}]
  (let [result (github-api-request token
                                   repo
                                   (str (get-remote-hooks-location hooks-dir) hook-name))]
    (if (:success result)
      (try
        (let [content (-> (:data result)
                         (:content)
                         (.getBytes "UTF-8")
                         (->> (.decode (Base64/getDecoder)))
                         (String. "UTF-8"))]
          (log/info "Downloaded:" hook-name)
          {:name    hook-name
           :content content})
        (catch Exception e
          (log/error "Failed to install hook:" (.getMessage e))
          {:error (.getMessage e)
           :name  hook-name}))
      result)))

;; =====gh cli=====

(defn- get-tmp-repo-directory
  "Given the repo-name return the tmp-directory location"
  [repo-name]
  (str "/tmp/kokki-" (str/replace repo-name #"\/" "-")))

(defn- git-available?
  "returns true if git is available locally"
  []
  (= 0 (:exit (p/sh "git" "--version"))))

(defn- clone-it!
  "given the repository make sure its cloned locally for integrating locally"
  [repo-name]
  (try
    (let [temp-dir      (get-tmp-repo-directory repo-name)
          ssh-url       (str "git@github.com:" repo-name ".git")
            clone-result  (if (fs/exists? (fs/path temp-dir))
                          {:exit 0}
                          (p/sh "git" "clone" "--depth" "1" ssh-url temp-dir))]
      (if (= 0 (:exit clone-result))
        {:temp-dir temp-dir}
        (log/error (str "Git clone failed: " (:err clone-result)))))
    (catch Exception e
      (log/error "Clone repository failed:" (Throwable->map e))
      {:error (.getMessage e)})))

;; =====protocol-impl=====

(defrecord TokenAuthGithub [pat-token repo-name hooks-dir]
  proto/IProvider

  (get-token [_]
    pat-token)

  (get-hooks [_]
    (let [result (github-api-request pat-token repo-name (get-remote-hooks-location hooks-dir))]
      (if-let [github-response (:data result)]
        (mapv #(select-keys % #{:name}) github-response) 
        result)))

  (list-hooks [this]
    (let [hooks (proto/get-hooks this)]
      {:hooks (->> (mapv :name hooks)
                   not-empty)}))

  (apply-hooks
    [this]
    (proto/apply-hooks this nil))
  
  (apply-hooks
    [this hook-name]
    (let [hooks-result (proto/get-hooks this)]
      (if-let [hooks (:hooks hooks-result)]
        (let [results (->> hooks
                           (filterv (utils/->hooks-filter-fn hook-name))
                           (mapv (partial download-hook-from-git (proto/get-token this)
                                                                 repo-name
                                                                 hooks-dir)))]
          (if (every? (complement :error) results)
            (do (utils/setup-hooks hooks-dir
                                   results)
                (utils/done-fn))
            {:error "Some hooks failed to sync"}))
        hooks-result))))

(defrecord SshGithubProvider [repo-name hooks-dir]
  proto/IProvider

  (get-token [_]
    (git-available?))

  (get-hooks [this]
    (if (proto/get-token this)
      (if-let [tmp-repo (->> (clone-it! repo-name)
                             :temp-dir)]
        (utils/get-list-of-hooks-from-directory (fs/path tmp-repo (utils/get-src-hooks-dir hooks-dir))) 
        (throw (RuntimeException. (str repo-name "not found locally at:" (get-tmp-repo-directory repo-name)))))
      (throw (RuntimeException. "git is not available locally!"))))

  (list-hooks [this]
    {:hooks (->> (mapv :name (proto/get-hooks this))
                 not-empty)})

  (apply-hooks 
    [this]
    (proto/apply-hooks this nil))

  (apply-hooks
    [this hook-name]
    (let [all-hooks (proto/get-hooks this)]
      (some->> all-hooks
               (filterv (utils/->hooks-filter-fn hook-name))
               (mapv (partial utils/setup-hooks hooks-dir))
               utils/done-fn))))
