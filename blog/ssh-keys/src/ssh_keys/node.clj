(ns ssh-keys.node
  (:use [pallet.core :only (defnode)]
        [pallet.resource :only (phase)]
        [pallet.crate.automated-admin-user
         :only (automated-admin-user)]
        clojure.pprint)
  (:require pallet.compute.vmfest
            [pallet.crate.ssh-key :as ssh-key]
            [pallet.resource.user :as user]
            [pallet.parameter :as parameter]
            [pallet.resource :as resource]
            [pallet.compute :as compute]
            [pallet.request-map :as request-map]))

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

(defn- get-master-host-id
  "Get the id of the 'master' node"
  [request master-tag]
  ;; assuming there is only one instance of the "master" node
  (let [[master-node] (request-map/nodes-in-tag request master-tag)]
    (compute/id master-node)))

(defn create-slave-user
  [request & {:keys [user master] :or {user "slave"
                                       master "master"}}]
  (let [master-id (get-master-host-id request master)
        ;; get the key for the master node from the parameters
        master-key
        (parameter/get-for
         request
         [:host (keyword master-id) :user (keyword master) :id_rsa])]
    (->
     request
     ;; create the slave user
     (user/user user
                :system true
                :create-home true
                :shell :bash)
     (ssh-key/authorize-key
      user
      master-key))))

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
              ;; [:host :<master-id> :user :master :id_rsa]
              (ssh-key/record-public-key "master")
              (debug "after record-public-key")))

(defnode slave
  {:os-family :ubuntu
   :os-64-bit true}
  :bootstrap automated-admin-user
  :auth-master (phase
                (debug "before config slave user")
                create-slave-user))

(comment
  (use 'pallet.core)
  (use 'pallet.compute)
  (def service (compute-service-from-config-file :virtualbox))
  (use 'ssh-keys.node)
  ;; create the configuration
  (converge {master 1 slave 3} :compute service :phase [:configure :auth-master])
  ;; $ ssh <master-ip>)
  ;; once in the master node
  ;; $ sudo su - master
  ;; $ ssh slave@<slave-ip>
  ;; notice how there was no password needed for the master to ssh
  ;; into the slave :)
  )