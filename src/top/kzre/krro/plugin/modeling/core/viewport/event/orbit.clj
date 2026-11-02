(ns top.kzre.krro.plugin.modeling.core.viewport.event.orbit
  "视口轨道旋转（orbit）的事件处理。

  只改 re-frame db 里的 :orbit/:azimuth、:orbit/:elevation 与
  :orbit/:flipped?，不进 ProjectScene / COW、不进 undo、不产生指令。
  计算全部委托 ViewportMath.orbitAngles；本命名空间只做坐标差、解构、
  调 Java、回写。

  语义：相机绕注视点旋转，target 与 distance 不变。
  :camera-view 模式下不响应（与 pan 一致）。

  瞬态策略：增量式。press 记录上一帧屏幕坐标；drag 用帧间增量，
  每帧从当前视口状态（角度 + flipped?）出发计算——不用 press 时的
  起始角度。

  为什么必须是增量式：越过极点时 flipped? 会 toggle，right/up 翻转，
  屏幕方向到世界方向的符号取反。绝对式用固定的 start-flipped 定符号，
  翻越后的位移方向必然错。增量式每帧用当前 flipped? 定符号，翻越后
  下一帧自动切换，方向正确。角度是标量，增量累积误差可忽略。

  灵敏度：拖拽一个视口高度旋转一圈，sensitivity = 2π / view-height。
  视口尺寸就绪由输入层保证。

  前提：
    - viewport/set-frame-viewport 负责写 db、重算 viewProj、推送给 Session。"
  (:require
    [top.kzre.krro.core.reframe.core :as rf]
    [top.kzre.krro.plugin.modeling.core.project.scene :as scene]
    [top.kzre.krro.plugin.painting.core.edit.tool :as tool :refer [cleanup-tool-interceptor]]
    [top.kzre.krro.plugin.modeling.core.viewport.frame :as frame])
  (:import
    [top.kzre.krro.plugin.modeling.core.viewport ViewportMath]))

;; 瞬态工具状态。只记上一帧屏幕坐标——增量式，下一帧的增量从它算，
;; 之后立刻用新坐标替换。不记角度：角度从当前视口状态读，这样
;; 每帧都用当前的 flipped? 定符号。
(defrecord OrbitToolData [last-x last-y]
  tool/IToolData
  (cleanup! [_ _ctx] nil)
  (overlay [_ _ctx] nil)
  (dispatch-event [_ _event-map _cofx] nil))

(defn- make-state []
  (map->OrbitToolData {}))

(rf/reg-event-fx
  scene/app-id :viewport-orbit/press
  [(cleanup-tool-interceptor OrbitToolData) :factory (fn [_] (make-state))]
  (fn [cofx [_ _scene-id event-map _frame]]
    (let [record (:record cofx)
          {:keys [x y]} event-map]
      ;; 只记录起点坐标，不改视口状态。
      {:record (assoc record (tool/tool-date-key)
                             (->OrbitToolData x y))})))

(rf/reg-event-fx
  scene/app-id :viewport-orbit/drag
  [(cleanup-tool-interceptor OrbitToolData)]
  (fn [cofx [_ _scene-id event-map frame
             ;; 再后续的参数整合成map传递
             {:keys [view-height]}]]
    (let [record (:record cofx)
          td     (get record (tool/tool-date-key))]
      (if (nil? td)
        {}                                   ; 非拖拽状态，忽略 stray 事件
        (let [vp (frame/get-viewport frame)]
          (if (not= :free-orbit (:mode vp))
            {}                               ; camera-view 下不响应 orbit
            (let [{:keys [x y]} event-map
                  dx (- x (:last-x td))       ; 帧间增量，不是相对 press 起点
                  dy (- y (:last-y td))
                  az-el (float-array 2)
                  ;; 拖拽一个视口高度旋转一圈
                  sensitivity (/ (* 2 Math/PI) view-height)
                  ;; 从当前角度 + 当前 flipped? 出发，增量式
                  flipped? (ViewportMath/orbitAngles
                             (double (get-in vp [:orbit :azimuth]))
                             (double (get-in vp [:orbit :elevation]))
                             (boolean (get-in vp [:orbit :flipped?]))
                             (double dx) (double dy) sensitivity
                             az-el)
                  vp' (-> vp
                          (assoc-in [:orbit :azimuth]   (aget az-el 0))
                          (assoc-in [:orbit :elevation] (aget az-el 1))
                          (assoc-in [:orbit :flipped?]  flipped?))
                  ;; 更新上一帧坐标，供下一次增量使用
                  record' (assoc record (tool/tool-date-key)
                                        (->OrbitToolData x y))]
              {:record record'
               :fx [[:viewport/set-frame-viewport frame vp']]})))))))

(rf/reg-event-fx
  scene/app-id :viewport-orbit/release
  (fn [_cofx _]
    ;; 清理由 cleanup-tool-interceptor 负责。与 pan 的 release 一致。
    nil))