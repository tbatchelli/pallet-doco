(defproject pallet-ssh-keys "0.0.1-SNAPSHOT"
  :description "An example pallet config for setting up ssh keys"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.cloudhoist/pallet "0.4.13-SNAPSHOT"]
                 [vmfest/vmfest "0.2.3-SNAPSHOT"]
                 [org.cloudhoist/automated-admin-user "0.4.1"]
                 [org.cloudhoist/ssh-key "0.4.1"]]
  :dev-dependencies [[swank-clojure/swank-clojure "1.2.1"]
                     [org.cloudhoist/pallet-lein "0.4.0"]]
  :repositories {"sonatype"
                 "https://oss.sonatype.org/content/repositories/releases/"
                 "sonatype-snapshots"
                 "https://oss.sonatype.org/content/repositories/snapshots"})
