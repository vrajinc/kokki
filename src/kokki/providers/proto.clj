(ns kokki.providers.proto
  "i define the protocols for providers")

(defprotocol IProvider

  (get-token [this]
             "returns the token _iff_ required for the provider")

  (get-hooks [this]
             "retrieves the hooks from the distributed / local system
             in the format of 
             [{:name ...}
              {:name ...}]")
  
  (list-hooks [this]
              "list all the available hooks with the provider")
  
  (apply-hooks [this]
               [this hook-name]
               "i apply the hooks available to local system
               (or)
               i apply the hooks specific to hook-name in local system"))
