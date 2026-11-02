(ns top.kzre.krro.plugin.modeling.core.viewport.event.orbit
  "视口轨道旋转（orbit）的事件处理。

  只改 re-frame db 里的 :orbit/:azimuth 与 :orbit/:elevation，
  不进 ProjectScene / COW、不进 undo、不产生指令。
  计算全部委托 ViewportMath.orbitAngles；本命名空间只做解构、坐标差、
  调 Java、回写。

  语义：相机绕注视点旋转，target 与 distance 不变。
  :camera-view 模式下不响应（与 pan 一致）。

  瞬态策略与 pan 相同：press 记录起点屏幕坐标 + 起始角度；
  drag 基于起点重算总增量（绝对式）；release 清空。

  灵敏度：当前用固定值 SENSITIVITY（弧度/像素）。
  后续可改为从全局配置读取，或按 view-height 推导
  （拖拽一个视口高度 ≈ 旋转一圈：2π / height）。

  前提：
    - viewport/set-frame-viewport 负责写 db、重算 viewProj、推送给 Session。"
  (:require
    [top.kzre.krro.core.reframe.core :as rf]
    [top.kzre.krro.plugin.modeling.core.project.scene :as scene]
    [top.kzre.krro.plugin.painting.core.edit.tool :as tool :refer [cleanup-tool-interceptor]]
    [top.kzre.krro.plugin.modeling.core.viewport.frame :as frame])
  (:import
    [top.kzre.krro.plugin.modeling.core.viewport ViewportMath]))

;; 瞬态工具状态。start-azimuth / start-elevation 是 press 时的角度，
;; drag 从它们重算，不是从上一帧累加——鼠标事件丢帧时仍然正确。
(defrecord OrbitToolData [start-x start-y start-azimuth start-elevation]
  tool/IToolData
  (cleanup! [_ _ctx] nil)
  (overlay [_ _ctx] nil)
  (dispatch-event [_ _event-map _cofx] nil))

(defn- make-state []
  (map->OrbitToolData {}))

;; 弧度/像素。0.01 约等于 360px 拖拽 ≈ 3.6 弧度，手感中等。
(def ^:private sensitivity 0.01)

(rf/reg-event-fx
  scene/app-id :viewport-orbit/press
  [(cleanup-tool-interceptor OrbitToolData) :factory (fn [_] (make-state))]
  (fn [cofx [_ _scene-id event-map frame]]
    (let [record (:record cofx)
          vp     (frame/get-viewport frame)
          {:keys [x y]} event-map]
      ;; 只记录起点与起始角度，不改视口状态。
      {:record (assoc record (tool/tool-date-key)
                             (->OrbitToolData x y
                                              (get-in vp [:orbit :azimuth])
                                              (get-in vp [:orbit :elevation])))})))

(rf/reg-event-fx
  scene/app-id :viewport-orbit/drag
  [(cleanup-tool-interceptor OrbitToolData)]
  (fn [cofx [_ _scene-id event-map frame]]
    (let [record (:record cofx)
          td     (get record (tool/tool-date-key))]
      (if (nil? td)
        {}                                   ; 非拖拽状态，忽略 stray 事件
        (let [vp (frame/get-viewport frame)]
          (if (not= :free-orbit (:mode vp))
            {}                               ; camera-view 下不响应 orbit
            (let [{:keys [x y]} event-map
                  dx (- x (:start-x td))     ; 相对 press 起点的总位移
                  dy (- y (:start-y td))
                  az-el (float-array 2)]
              ;; 起始角度 + 屏幕位移 → 新角度（钳制 elevation）
              (ViewportMath/orbitAngles
                (double (:start-azimuth td))
                (double (:start-elevation td))
                (double dx) (double dy) sensitivity
                az-el)
              (let [vp' (-> vp
                            (assoc-in [:orbit :azimuth]   (aget az-el 0))
                            (assoc-in [:orbit :elevation] (aget az-el 1)))]
                {:fx [[:viewport/set-frame-viewport frame vp']]}))))))))

(rf/reg-event-fx
  scene/app-id :viewport-orbit/release
  (fn [_cofx _]
    ;; 清理由 cleanup-tool-interceptor 负责。与 pan 的 release 一致。
    nil))