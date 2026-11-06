(ns top.kzre.krro.plugin.modeling.core.scene.subs
  (:require
    [top.kzre.krro.core.reframe.core :as rf]
    [top.kzre.krro.plugin.modeling.core.project.scene :as scene]))

(rf/reg-sub scene/app-id
  :scene-accel
  :<- [:record]
  scene/scene-accel)