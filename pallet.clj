;;; Pallet project configuration file

(require
 '[pallet.crate.docker-test :refer [live-test-spec]]
 '[pallet.crates.test-nodes :refer [node-specs]])

(defproject docker-crate
  :provider node-specs                  ; supported pallet nodes
  :groups [(group-spec "docker-live-test"
             :extends [with-automated-admin-user
                       live-test-spec]
             :roles #{:live-test :default :docker})])
