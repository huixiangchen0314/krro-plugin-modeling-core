(ns top.kzre.krro.plugin.modeling.core.viewport.effects
  (:require
   [top.kzre.krro.core.reframe.reframe :as rf]
   [top.kzre.krro.plugin.modeling.core.project.scene :as scene]
   [top.kzre.krro.plugin.modeling.core.viewport.frame :as frame]))

;; 副作用，设置视口
(rf/reg-fx
  scene/app-id :viewport/set-frame-viewport
  (fn [_ frame viewport]
    (frame/set-viewport! frame viewport)))