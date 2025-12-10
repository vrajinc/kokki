(ns kokki.cli
  "i handle CLI stuff"
  (:require [kokki.constant :as const]
            [clojure.string :as str]
            [babashka.cli :as cli]
            [babashka.fs :as fs]))

(def cli-options
  "Command line options configuration"
  {:spec {:help       {:alias   :h
                       :desc    "show help"
                       :coerce  :boolean}
          :verbose    {:alias   :v
                       :desc    "Verbose output"
                       :coerce  :boolean}
          :repo       {:alias     :r
                       :desc      "GitHub repository (owner/repo)"
                       :validate  (every-pred #(re-matches #"[^/]+/[^/]+" %)
                                              not-empty)}
          :token      {:alias     :t
                       :validate  some?
                       :desc      "GitHub personal access token"}
          :ssh        {:alias     :s
                       :desc      "Use SSH authentication (via GitHub CLI or git)"
                       :coerce    :boolean}
          :src-dir    {:desc          "the hooks source dir to lookup"
                       :validate      some?
                       :default       const/hooks-location}
          :hooks-dir  {:alias         :d
                       :desc          "Local hooks directory"
                       :validate      some?
                       :default       "./.git/hooks"}}})

(defn usage 
  "Generate usage string for CLI help"
  [options-summary]
  (->> ["Kokki - Distributed GitHub Hooks Management"
        ""
        "Usage: kokki [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  setup    Setup local hooks directory"
        "  list     List available hooks"
        "  install  Install a specific hook"
        ""
         "Examples:"
         "  kokki -r owner/repo -t token setup # Using PAT token"
         "  kokki -r owner/repo --ssh setup # Using SSH authentication"
         "  kokki -d ./hooks list"]
       (str/join const/NEW-LINE)))

(defn error-msg 
  "Format error message for CLI parsing errors"
  [errors]
  (str "The following errors occurred while parsing your command:"
       const/NEW-LINE
       const/NEW-LINE
       (str/join const/NEW-LINE errors)))

(defn validate-args
  "Validate command line arguments. Eitherreturn a map indicating the program
  should exit (with an error message), or a map indicating the action the
  program should take."
  [args]
  (let [{:keys [opts args]} (cli/parse-args args cli-options)
        {:keys [token
                repo
                src-dir
                ssh]} opts
        summary             (cli/format-opts cli-options)
        user-action         (->> (first args)
                                 keyword)
        user-agent          (cond
                              (some? token)   :github-pat
                              ssh             :github-ssh
                              (some? src-dir) :inline
                              :else           :unknown)
        specific-hook       (second args)]
    (cond
      (:help opts) ; help => exit OK with usage summary
      {:exit-message  (usage summary)
       :ok?           true}

      (and (#{:github-pat
              :github-ssh} user-agent)
           (nil? repo))
      {:exit-message  (error-msg [(str "missing repository name"
                                       const/NEW-LINE
                                       (usage summary))])}
      
      (empty? args) ; no action specified
      {:exit-message  (error-msg [(str "No action specified"
                                       const/NEW-LINE
                                       const/NEW-LINE
                                       (usage summary))])
       :ok?           false}

      (nil? (const/supported-actions? user-action))
      {:exit-message  (error-msg [(str "Unsupported action"
                                       user-action
                                       const/NEW-LINE
                                       (usage summary))])
       :ok?           false}

      (nil? (const/supported-providers? user-agent))
      {:exit-message  (error-msg [(str "Unsupported provider"
                                       const/NEW-LINE
                                       user-agent
                                       const/NEW-LINE
                                       (usage summary))])
       :ok?           false}
      
      :else ; valid arguments
      {:action        user-action
       :provider      user-agent
       :specific-hook specific-hook
       :options       opts})))
