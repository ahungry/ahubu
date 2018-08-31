(defproject ahubu "0.1.0-SNAPSHOT"
  :description "The best browser"
  :url "https://github.com/ahungry/ahubu"
  :license {:name "GPLv3 or Later"
            :url "http://www.gnu.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :java-source-paths ["java-src"]
  :main ahubu.core
  :aot [ahubu.lib]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-Dfile.encoding=UTF8"])
