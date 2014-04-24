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
  {:ufw-file "/etc/default/ufw"
   :ufw-settings {:DEFAULT_FORWARD_POLICY "ACCEPT"}})

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
            :package-source
            (:package-source settings
                             {:name "docker"
                              :apt {:scopes ["main"]
                                    :release "docker"
                                    :url "http://get.docker.io/ubuntu"
                                    :key-url "https://get.docker.io/gpg"}})
            :packages ["lxc-docker"]
            ::packages ["linux-image-generic-lts-raring"
                        "linux-headers-generic-lts-raring"]
            ::reboot true)))

(defmethod-version-plan
    settings-map {:os :ubuntu :os-version [[13 04]]}
    [os os-version version settings]
  (cond
   (:install-strategy settings) settings
   :else  (assoc settings
            :install-strategy :package-source
            :package-source
            (:package-source settings
                             {:name "docker"
                              :apt {:scopes ["main"]
                                    :release "docker"
                                    :url "http://get.docker.io/ubuntu"
                                    :key-url "https://get.docker.io/gpg"}})
            :packages ["lxc-docker"]
            ::packages ["linux-image-extra-`uname -r`"])))

(defplan settings
  "Settings for docker"
  [{:keys [user owner group dist dist-urls version instance-id]
    :as settings}
   & {:keys [instance-id] :as options}]
  (let [settings (merge (default-settings options) settings)
        settings (settings-map (:version settings) settings)]
    (debugf "docker settings %s" settings)
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
(defn ufw-config
  "Produce a ufw config file based on a map of configuration values."
  [settings]
  (string/join \newline
               (map (fn [[k v]] (str (name k) "=\"\"" v "\"")) settings)))

(defplan configure-ufw
  [path ufw-settings]
  (remote-file path :content (ufw-config ufw-settings)))

(defplan configure
  "Write all config files"
  [{:keys [instance-id] :as options}]
  (let [{:keys [config ufw-file ufw-settings] :as settings}
        (get-settings :docker options)]
    (configure-ufw ufw-file ufw-settings)))

;;; # Commands
(defplan nodes
  "Return a list of nodes"
  []
  (let [res (exec-script
             (pipe ("docker" ps --no-trunc)
                   ("tail" -n "+2")
                   ("awk" "'{ print $1 }'")
                   ("xargs"  --no-run-if-empty docker inspect)))]
    (with-action-values [res]
      (if (zero? (:exit res))
        {:exit 0
         :out (-> (:out res)
                  yaml/parse-string
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
  (debugf "run %s %s %s" image-id cmd options)
  (let [opt-string (-> (dissoc options :port)
                       (translate-options run-options)
                       map-to-arg-string)
        port-string (string/join " " (map #(str "-p " %) (:port options)))
        res (exec-checked-script
             "docker run"
             (pipe
              ("docker" run ~opt-string ~port-string ~image-id ~cmd)
              ("xargs" docker inspect)))]
    (with-action-values [res]
      (if (zero? (:exit res))
        ;; remove everything before first {, such as downloading messages
        ;; from the image being pulled
        (let [out (string/replace-first (:out res) #"(?m)[^{}]*\{" "[{")]
          (debugf "run %s" out)
          (try
            {:exit 0
             :out (first (yaml/parse-string out))}
            (catch Exception e
              {:exit 1
               :exception e})))
        {:exit 1}))))

(defplan kill
  "kill a container"
  [id]
  (exec-checked-script
   "docker kill"
   ("docker" kill ~id)))

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
                         (configure options))}
   :default-phases [:install :configure]))
