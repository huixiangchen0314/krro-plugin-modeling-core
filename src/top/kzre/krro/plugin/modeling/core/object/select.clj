(ns top.kzre.krro.plugin.modeling.core.object.select
  "对象选择"
  (:require
    [top.kzre.krro.core.reframe.core :as rf]
    [top.kzre.krro.plugin.modeling.core.project.scene :as scene]
    [top.kzre.krro.plugin.modeling.core.viewport.core :as viewport])
  (:import (top.kzre.krro.d3.core.geometry Ray)
           (top.kzre.krro.plugin.modeling.core.object Select)
           (top.kzre.krro.plugin.modeling.core.project.accel RayAccel)
           (top.kzre.krro.plugin.modeling.core.viewport ScreenTransform)))



;; 物体单选
(rf/reg-event-fx
  scene/app-id :object-select/select
  (fn [cofx [_ scene-id
             {:keys [x y]
              :as _event-map} frame
             ;; 再后续的参数整合成map传递
             {:keys [view-height view-width]}]]
    (let [vp (assoc (viewport/get-viewport frame)
               :aspect (double (/ view-width view-height)))
          ^RayAccel scene-accel @(scene/subscribe scene-id :scene-accel)
          view-proj-inv (viewport/viewport->view-proj-inv vp)
          ^Ray ray (ScreenTransform/screenPointToWorldRay
                view-proj-inv x y view-width view-height)
          obj-candidates (.query scene-accel ray)]
      (if (> (alength obj-candidates) 0)
       (println "hit count: " (alength obj-candidates))
       (println "miss object"))
      (when-let [selected (Select/select scene-id ray obj-candidates)]
        (println "select: " selected))
      )
    (let [record (:record cofx)]
      {:record (assoc record
                 :selected-objects []
                 :active-object nil)})))

;; 物体多选
(rf/reg-event-fx
  scene/app-id :object-select/multi-select
  (fn [cofx [_ scene-id]]
    (let [record (:record cofx)]
      {:record (assoc record
                 :selected-objects []
                 :active-object nil)})))