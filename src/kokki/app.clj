(ns kokki.app
  "kokki- application"
  (:require [kokki.cli :as cli]
            [kokki.core :as kokki])
  (:gen-class))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [exit-message ok?]
         :as parsed-input} (cli/validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [parsed-input  (select-keys parsed-input
                                       #{:action
                                         :provider
                                         :specifi-hook
                                         :options})
            Provider      (kokki/init-provider! (:provider parsed-input)
                                                parsed-input)
            result        (kokki/execute-action Provider parsed-input)]
        (if (:error result)
          (do
            (println "Error:" (:error result))
            (System/exit 1))
          (do
            (when (:message result)
              (println (:message result)))
            (when (not-empty (:hooks result))
              (do (println "Available hooks:")
                  (doseq [hook (:hooks result)]
                    (println "-" hook))))
            (System/exit 0)))))))
