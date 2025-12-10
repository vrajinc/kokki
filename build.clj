(ns build
    (:require [clojure.string :as str]
              [clojure.tools.build.api :as b]
              [deps-deploy.deps-deploy :as dd]))

(def group "vraj.inc")
(def app-name "kokki")
(def version "1.0.0")
(def about "setup hooks, its awesome")

(def lib (symbol group app-name))
(def target-dir "target")
(def class-dir (format "%s/classes" target-dir))
(def pom-file (format "%s/classes/META-INF/maven/%s/%s/pom.xml" target-dir group app-name))
(def uberjar-file (format "%s/%s-standalone.jar" target-dir app-name))
(def about-dir (format "%s/about" target-dir))

(defn ci-print
  [& args]
  (apply println (format "[build.clj: %s]" app-name) args))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean
  [params]
  (ci-print "cleaning up")
  (b/delete {:path "target"})
  params)

(defn install [_]
  (ci-print (format "installing %s %s to local M2" lib version))
  (b/install {:basis @basis
              :lib lib
              :version version
              :jar-file uberjar-file
              :class-dir class-dir}))

(defn uberjar
  [_]
  (clean nil)
  (ci-print (format "building ubejar for %s %s" app-name version))
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :ns-compile [(symbol (str app-name ".app"))]
                  :class-dir class-dir})
  (b/uber {:basis @basis
           :class-dir class-dir
           :uber-file uberjar-file
           :main (symbol (str "vraj.inc." (str/capitalize app-name)))})
  (ci-print "uberjar successfully saved at" uberjar-file))

(defn deploy-jar
  [_]
  (ci-print (format "deploying jar for %s %s" app-name version))
  (dd/deploy {:installer :remote
              :artifact uberjar-file
              :pom-file pom-file
              ;; Deps-deploy can use a string ("multipass-snapshots") for reading details from
              ;; deps.edn and credentials from ~/.m2/settings.xml but our pipeline does not set
              ;; that. That is why we override explicitly here.
              }))

