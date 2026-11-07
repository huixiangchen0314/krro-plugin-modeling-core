(ns top.kzre.krro.plugin.modeling.core.object.subs
  (:require
    [top.kzre.krro.core.reframe.reframe :as rf]
    [top.kzre.krro.plugin.modeling.core.project.scene :as scene])
  (:import (top.kzre.krro.plugin.modeling.core.object IActiveObjectHolder IActiveObjectHolderFactory)))

(rf/reg-sub
  scene/app-id :active-object
  :<- [:record]
  :active-object)

(defrecord ActiveObjectHolder [sig]
  IActiveObjectHolder
  (activeObject [_]
    @sig))

(defn- active-object-holder [scene-id]
  (let [sig (scene/subscribe scene-id :active-object)]
    (->ActiveObjectHolder sig)))

(defrecord ActiveObjectHolderFactory []
  IActiveObjectHolderFactory
  (create [_ scene-id]
    (active-object-holder scene-id)))

(def ^:private active-object-holder-factory*
  (->ActiveObjectHolderFactory))

(defn active-object-holder-factory []
  active-object-holder-factory*)