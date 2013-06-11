(defproject com.palletops/docker-crate "0.8.0-alpha.1"
  :description "Crate for docker installation"
  :url "http://github.com/pallet/docker-crate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.palletops/pallet "0.8.0-RC.1"]
                 [clj-yaml "0.4.0"]]
  :resource {:resource-paths ["doc-src"]
             :target-path "target/classes/pallet_crate/docker_crate/"
             :includes [#"doc-src/USAGE.*"]}
  :prep-tasks ["resource" "crate-doc"])
