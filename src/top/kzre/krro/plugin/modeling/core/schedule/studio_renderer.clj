(ns top.kzre.krro.plugin.modeling.core.schedule.studio-renderer
  (:require
    [top.kzre.krro.core.util.computing-graph :as cg])
  (:import (top.kzre.krro.plugin.modeling.core.schedule SceneRenderer)))

;; 渲染器参与调度图
(defrecord StudioRenderNode []
  ;; 场景渲染器适配接口
  SceneRenderer

  ;; 计算图节点
  cg/INode)