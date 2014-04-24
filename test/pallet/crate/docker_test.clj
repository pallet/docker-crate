(ns pallet.crate.docker-test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.tools.logging :refer [debugf]]
   [pallet.actions :refer [as-action assoc-settings package-manager
                           with-action-values]]
   [pallet.argument :refer [delayed]]
   [pallet.algo.fsmop :refer [complete?]]
   [pallet.api :refer [plan-fn server-spec]]
   [pallet.build-actions :as build-actions]
   [pallet.crate :refer [get-settings]]
   [pallet.crate.docker :as docker]
   [pallet.crate.network-service :refer [wait-for-port-listen]]))

(deftest invoke-test
  (is (build-actions/build-actions
          {:server {:image {:os-family :ubuntu :os-version "13.04"}}}
        (docker/settings {})
        (docker/install {})
        (docker/configure {}))))

(def live-test-spec
  (server-spec
   :extends [(docker/server-spec {})]
   :phases {:bootstrap (plan-fn (package-manager :update))
            :test-run (plan-fn
                        (let [v (docker/run
                                 "pallet/ubuntu2" "/usr/sbin/sshd -D"
                                 {:detached true})
                              n (docker/nodes)
                              id (with-action-values [v]
                                   (debugf "node id is %s" (:ID (:out v)))
                                   {:id (:ID (:out v))})]
                          (assoc-settings :docker-test id)
                          (let [n2 (docker/nodes)]
                            (with-action-values [n n2 id]
                              (debugf "nodes %s" (:out n))
                              (assert (= 1 (count (:out n))) "run failed")
                              (assert (= (:id id) (:ID (first (:out n))))
                                      "incorrect id")))))
            :test-kill (plan-fn
                         (let [{:keys [id]} (get-settings :docker-test)]
                           (docker/kill id)
                           (let [n (docker/nodes)]
                             (with-action-values [n]
                               (assert (zero? (count (:out n)))
                                       "kill failed")))))}
   :default-phases [:install :configure :test-run :test-kill]))
