(ns top.kzre.krro.plugin.modeling.core.viewport.frame
  "视口数据在 frame 参数中的规范存取点。

  视口子系统跨帧传递的状态都经过这个命名空间：
    ::viewport         视口状态（orbit 参数、投影、模式）—— 结构由 spec 定义
    ::viewport-width   视口宽，像素
    ::viewport-height  视口高，像素

  width / height 和视口状态分开存，因为生命周期不同：
    - 视口状态：用户输入（导航/投影切换），高频写、随项目持久化
    - width / height：布局属性，低频写（resize）、不持久化

  aspect = width / height 是派生值，不单独存——避免和 width/height 不一致。
  由 get-aspect 现算。

  尺寸的真相来源是 Java Session.setViewSize；这里存一份副本供 Clojure 侧
  的矩阵计算和 pan 计算使用。两者在 resize 事件里同步更新。

  get-viewport 在未设置时返回新构造的默认视口（不存储）。任何修改视口的
  调用方必须显式 set-viewport! ——读默认值不产生持久状态。"
  (:require
   [top.kzre.krro.core.frame :as frame :refer [IFrame]]
   [top.kzre.krro.plugin.modeling.core.viewport.spec :as spec])
  )

;; ============================================================================
;; 键定义
;; ============================================================================

(def ^:private viewport-key*        ::viewport)
(def ^:private viewport-width-key*  ::viewport-width)
(def ^:private viewport-height-key* ::viewport-height)


;; ============================================================================
;; 视口状态
;; ============================================================================

(defn get-viewport
  "返回 frame-or-map 上的视口状态。未设置时返回新构造的默认视口，不存储。
   需要持久化的修改必须跟 set-viewport!。"
  [frame-or-map]
  {:pre [(some? frame-or-map)]}
  (or (if (satisfies? IFrame frame-or-map)
        (frame/param frame-or-map viewport-key*)
        (get frame-or-map viewport-key*))
      (spec/make-viewport)))

(defn set-viewport!
  "存储视口状态。"
  [frame viewport]
  {:pre [(some? viewport)]}
  (frame/set-param! frame viewport-key* viewport))
