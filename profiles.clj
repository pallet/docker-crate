{:dev
 {:dependencies [[com.palletops/pallet "0.8.0-RC.9" :classifier "tests"]
                 [com.palletops/crates "0.1.1"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :plugins [[com.palletops/lein-pallet-crate "0.1.0"]
            [codox/codox.leiningen "0.6.4"]
            [lein-marginalia "0.7.1"]
            [lein-pallet-release "RELEASE"]]}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.8/api"
               :src-dir-uri "https://github.com/pallet/docker-crate/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/0.8/annotated"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}}
