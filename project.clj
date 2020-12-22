(defproject portcard-api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]

                 ;; firebase
                 [com.google.firebase/firebase-admin "7.0.1"]

                 ;; for handler
                 [ring/ring-jetty-adapter "1.8.2"]
                 [metosin/reitit "0.5.10"]
                 [metosin/reitit-swagger "0.5.10"]
                 [metosin/reitit-swagger-ui "0.5.10"]

                 [ring-cors "0.1.13"]
                 [ring-logger "1.0.1"]
                 [com.fasterxml.jackson.core/jackson-core "2.11.3"] ;; required!!!

                 ;; for security
                 [buddy/buddy-hashers "1.6.0"]

                 ;; for json
                 [clj-time "0.15.2"]
                 [cheshire "5.10.0"]
                 ;; to deal with  environment variables
                 [environ "1.2.0"]

                 ;; for integrant-repl
                 [integrant "0.8.0"]
                 [integrant/repl "0.3.2"]

                 ;; for logging
                 [com.taoensso/timbre "5.1.0"]
                 [com.fzakaria/slf4j-timbre "0.3.20"]

                 ;; for database
                 [honeysql "1.0.444"]
                 [seancorfield/next.jdbc "1.1.610"]
                 [hikari-cp "2.13.0"]
                 [org.postgresql/postgresql "42.2.18"]
                 [net.ttddyy/datasource-proxy "1.5"]

                 ;; for migration
                 [ragtime "0.8.0"]

                 ;; for others
                 [camel-snake-kebab "0.4.2"]

                 [orchestra "2020.09.18-1"]]

  :main ^:skip-aot portcard-api.core
  :target-path "target/%s"

  :plugins [[lein-environ "1.1.0"]
            [cider/cider-nrepl "0.25.4"]
            [refactor-nrepl "2.5.0"]]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}

  :repl-options
  {:host "0.0.0.0"
   :port 39998})
