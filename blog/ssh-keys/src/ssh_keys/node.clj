(ns ssh-keys.node
  (:use [pallet.core :only (defnode)]
        [pallet.resource :only (phase)]
        [pallet.crate.automated-admin-user
         :only (automated-admin-user)]
        clojure.pprint)
  (:require pallet.compute.vmfest
            [pallet.crate.ssh-key :as ssh-key]
            [pallet.resource.user :as user]
            [pallet.parameter :as parameters]))

(defn create-master-user
  [request & {:keys [user] :or {user "master"}}]
  (->
   request
   ;; create the master user
   (user/user user
              :system true
              :create-home true
              :shell :bash)
   ;; generates the key for the user in the master node
   (ssh-key/generate-key user :comment "master_key")))

(defn create-slave-user
  [request & {:keys [user master] :or {user "slave"
                                       master "master"}}]
  (println (:parameters request))
  (let [master-key
        (parameters/lookup
         request
         [:host :master :user (keyword master) :id_rsa])]
    (->
     request
     ;; create the slave user
     (user/user user
                :system true
                :create-home true
                :shell :bash)
     (ssh-key/authorize-key
      user
      (parameters/lookup
       request
       [:host :master :user (keyword master) :id_rsa])))))

(defn debug [req & comment]
  (when comment (println "***" comment))
  (pprint req)
  req)

(defnode master
  {:os-family :ubuntu
   :os-64-bit true}
  :bootstrap (phase
              automated-admin-user
              create-master-user)
  :configure (phase
              ;; pulls the key and stores it into
              ;; [:parameters :master :id_rsa]
              ;;(debug "before")
              (ssh-key/record-public-key "master")
              ;;(debug "after")
              ))

(defnode slave
  {:os-family :ubuntu
   :os-64-bit true}
  :bootstrap automated-admin-user
  :authorize-master create-slave-user)

(comment
  (use 'pallet.core)
  (use 'pallet.compute)
  (def service (compute-service-from-config-file :virtualbox))
  (use 'ssh-keys.node)
  (converge {master 1 slave 3} :compute service :environment local-env))