(ns kokki.constant)

(def supported-actions?
  "set of supported user actions"
  #{:setup
    :list
    :install})

(def supported-providers?
  "set of supported hooks provider"
  #{:github-pat
    :github-ssh
    :inline})

(defonce NEW-LINE
  "\n")

(defonce hooks-location
  ".github/hooks/")
