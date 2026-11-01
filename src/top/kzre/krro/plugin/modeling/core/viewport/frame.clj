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
  (:require [top.kzre.krro.core.frame :as frame]
            [top.kzre.krro.plugin.modeling.core.viewport.spec :as spec]))

;; ============================================================================
;; 键定义
;; ============================================================================

(def ^:private viewport-key*        ::viewport)
(def ^:private viewport-width-key*  ::viewport-width)
(def ^:private viewport-height-key* ::viewport-height)

(defn viewport-key [] viewport-key*)

;; ============================================================================
;; 视口状态
;; ============================================================================

(defn get-viewport
  "返回 frame 上的视口状态。未设置时返回新构造的默认视口，不存储。
   需要持久化的修改必须跟 set-viewport!。"
  [frame]
  (or (frame/param frame viewport-key*)
      (spec/make-viewport)))

(defn set-viewport!
  "存储视口状态。"
  [frame viewport]
  {:pre [(some? viewport)]}
  (frame/set-param! frame viewport-key* viewport))

;; ============================================================================
;; 视口尺寸
;; ============================================================================

(defn get-viewport-width
  "返回视口宽度（像素）。未设置时返回 nil。"
  [frame]
  (frame/param frame viewport-width-key*))

(defn get-viewport-height
  "返回视口高度（像素）。未设置时返回 nil。"
  [frame]
  (frame/param frame viewport-height-key*))

(defn set-viewport-size!
  "同时存储视口宽度与高度。两个值必须原子更新——分开写会产生
   新 width + 旧 height 的中间态，导致 aspect 短暂错误。"
  [frame width height]
  {:pre [(pos? width) (pos? height)]}
  (frame/set-param! frame viewport-width-key* width)
  (frame/set-param! frame viewport-height-key* height))

(defn get-aspect
  "返回 aspect = width / height。宽度或高度未设置、或高度为 0 时返回 nil。
   nil 表示视口尺寸未就绪——调用方（如矩阵计算）应跳过本帧计算，
   而不是用默认值兜底（默认 1.0 会产生视觉畸变且难排查）。"
  [frame]
  (when-let [w (get-viewport-width frame)]
    (when-let [h (get-viewport-height frame)]
      (when (pos? h)
        (/ (double w) (double h))))))