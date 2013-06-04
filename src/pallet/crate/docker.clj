(ns pallet.crate.docker
  "A pallet crate to install and configure docker"
  (:require
   [clj-yaml.core :as yaml]
   [clojure.string :as string :refer [blank? split]]
   [clojure.tools.logging :refer [debugf]]
   [pallet.action :refer [with-action-options]]
   [pallet.actions :refer [as-action directory exec-checked-script exec-script
                           packages plan-when remote-directory remote-file
                           with-action-values]]
   [pallet.api :refer [plan-fn] :as api]
   [pallet.crate :refer [assoc-settings defmethod-plan defplan get-settings
                         service-phases]]
   [pallet.crate-install :as crate-install]
   [pallet.crate.nohup]
   [pallet.crate.service
    :refer [supervisor-config supervisor-config-map] :as service]
   [pallet.utils :refer [apply-map maybe-update-in]]
   [pallet.script.lib :refer [config-root file log-root translate-options]]
   [pallet.stevedore :refer [fragment map-to-arg-string]]
   [pallet.version-dispatch :refer [defmethod-version-plan
                                    defmulti-version-plan]]))


(def ^{:doc "Flag for recognising changes to configuration"}
  docker-config-changed-flag "docker-config")

;;; # Settings
(defn default-settings [options]
  {:ppa {:url "ppa:dotcloud/lxc-docker"}})

;;; At the moment we just have a single implementation of settings,
;;; but this is open-coded.
(defmulti-version-plan settings-map [version settings])

(defmethod-version-plan
    settings-map {:os :ubuntu :os-version [12 04]}
    [os os-version version settings]
  (cond
   (:install-strategy settings) settings
   :else  (assoc settings
            :install-strategy :package-source
            :package-source {:name "dotcloud" :apt (:ppa settings)}
            :packages ["lxc-docker"]
            ::packages ["linux-image-generic-lts-raring"]
            ::reboot true)))

(defmethod-version-plan
    settings-map {:os :ubuntu :os-version [13 04]}
    [os os-version version settings]
  (cond
   (:install-strategy settings) settings
   :else  (assoc settings
            :install-strategy :package-source
            :package-source {:name "dotcloud" :apt (:ppa settings)}
            :packages ["lxc-docker"]
            ::packages ["linux-image-extra-`uname -r`"])))

(defplan settings
  "Settings for docker"
  [{:keys [user owner group dist dist-urls version instance-id]
    :as settings}
   & {:keys [instance-id] :as options}]
  (let [settings (merge (default-settings options) settings)
        settings (settings-map (:version settings) settings)]
    (assoc-settings :docker settings {:instance-id instance-id})))


;;; # Install
(defplan install
  "Install docker."
  [{:keys [instance-id]}]
  (let [{:keys [owner group log-dir] :as settings}
        (get-settings :docker {:instance-id instance-id})]
    (packages :apt (::packages settings))
    (plan-when (::reboot settings)
      (exec-script ("reboot"))
      (as-action
       (Thread/sleep 30000)))
    (crate-install/install :docker instance-id)))

;;; # Configuration
(defplan configure
  "Write all config files"
  [{:keys [instance-id] :as options}]
  (let [{:keys [config] :as settings} (get-settings :docker options)]
    ))

;;; # Commands
(defplan nodes
  "Return a list of nodes"
  []
  (let [res (exec-script
             (pipe ("docker" ps -notrunc)
                   ("tail" -n "+2")
                   ("awk" "'{ print $1 }'")
                   (while ("read" uuid) ("docker" inspect @uuid) (println))))]
    (with-action-values [res]
      (if (zero? (:exit res))
        {:exit 0
         :out (->> (split (:out res) #"(?m)^\{")
                   (remove blank?)
                   (map #(str "{" %))
                   (map yaml/parse-string)
                   vec)}))))

(def run-options {:interactive :i
                  :ptty :t
                  :cpu-shares :c
                  :detached :d
                  :hostname :h
                  :memory :m
                  :port :p
                  :username :u
                  :volumes :v})

(defplan run
  "Run a container"
  [image-id cmd options]
  (let [opt-string (-> options
                       (translate-options run-options)
                       map-to-arg-string)
        res (exec-script
             (pipe
              ("docker" run ~opt-string ~image-id ~cmd)
              ("xargs" docker inspect)))]
    (with-action-values [res]
      (if (zero? (:exit res))
        ;; remove everything before first {, such as downloading messages
        ;; from the image being pulled
        (let [out (string/replace-first (:out res) #"(?m)[^{}]*\{" "{")]
          (debugf "run %s" out)
          (try
            {:exit 0
             :out (yaml/parse-string out)}
            (catch Exception e
              {:exit 1
               :exception e})))
        {:exit 1}))))

(defplan kill
  "kill a container"
  [id]
  (exec-script ("docker" kill ~id)))

(defplan commit
  "Commit a container"
  [id {:keys [author m run tag] :as options}]
  (let [options (maybe-update-in options [:run]
                                 #(when (string? %) (yaml/generate-string %)))]
    (exec-script ("docker" commit ~(map-to-arg-string (dissoc options :tag))
                  ~id ~(:tag options "")))))

;;; # Server Spec
(defn server-spec
  "Returns a server-spec that installs and configures docker."
  [settings & {:keys [instance-id] :as options}]
  (api/server-spec
   :phases {:settings (plan-fn
                        (pallet.crate.docker/settings (merge settings options)))
            :install (plan-fn
                       (install options))
            :configure (plan-fn
                         (configure options))}))
