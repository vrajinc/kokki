(ns kokki.core
  "Kokki - Distributed GitHub Hooks Management"
  (:require [kokki.providers.github :as github]
            [kokki.providers.inline :as inline]
            [kokki.providers.proto :as proto]
            [kokki.constant :as const]
            [clojure.tools.logging :as log]))

(defn init-provider!
  "given the type of provider will return the instance of Provider"
  [provider {{:keys [token
                     repo
                     src-dir
                     hooks-dir]} :options}]
  (case provider
    :github-pat (github/->TokenAuthGithub
                  token
                  repo
                  {:src     src-dir
                   :target  hooks-dir})
    :github-ssh (github/->SshGithubProvider
                  repo
                  {:src     src-dir
                   :target  hooks-dir})
    :inline     (inline/->InlineProvider
                  {:src     src-dir
                   :target  hooks-dir})))

(defn execute-action
  "Execute the specified action"
  [Provider
   {:keys [action
           provider
           options
           specific-hook]}]
  (let [{:keys [verbose]} options]
    (when verbose
      (log/info "Executing action:" action "with options:" (dissoc options :token))
      (log/info "Authentication method:" provider))
    (case action
      
      :list     (merge (proto/list-hooks Provider)
                       {:message const/NEW-LINE})
      
      :setup    (or (some->> (proto/apply-hooks Provider)
                             (str "Performing " action)
                             (hash-map :message))
                    {:error (str "Unable to perform " action)})

      :install  (or (some->> specific-hook
                             (proto/apply-hooks Provider)
                             (hash-map :message))
                    {:error (str "Unable to " action " :" specific-hook)}))))
