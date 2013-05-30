(ns pallet.crate.docker-test
  (:require
   [clojure.test :refer [deftest is]]
   [pallet.crate.docker :as docker]
   [pallet.actions :refer [package-manager]]
   [pallet.algo.fsmop :refer [complete?]]
   [pallet.api :refer [plan-fn server-spec]]
   [pallet.build-actions :as build-actions]
   [pallet.crate.java :as java]
   [pallet.crate.network-service :refer [wait-for-port-listen]]
   [pallet.crate.nohup :as nohup]
   [pallet.crate.runit :as runit]
   [pallet.crate.upstart :as upstart]))

(deftest invoke-test
  (is (build-actions/build-actions {}
        (docker/settings {})
        (docker/install {})
        (docker/configure {}))))

(def live-test-spec
  (server-spec
   :extends [(docker/server-spec {})]
   :phases {:install (plan-fn (package-manager :update))
            :test (plan-fn )}))
