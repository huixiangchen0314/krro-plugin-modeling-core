(ns top.kzre.krro.plugin.modeling.core.scene.event.accel
  "加速结构事件"
  (:require
    [top.kzre.krro.core.reframe.core :as rf]
    [top.kzre.krro.plugin.modeling.core.project.accel :as accel]
    [top.kzre.krro.plugin.modeling.core.project.scene :as scene])
  (:import (top.kzre.krro.plugin.modeling.core.scene.event Accel)))

;; 重建场景查询
(rf/reg-event-fx
  scene/app-id :scene-accel/rebuild
  (fn [cofx [_ scene-id]]
    (let [record (:record cofx)
          factory (accel/scene-accel-factory)
          accel (Accel/buildSceneAccel factory scene-id)]

      {:record (assoc record :scene-accel accel)})))