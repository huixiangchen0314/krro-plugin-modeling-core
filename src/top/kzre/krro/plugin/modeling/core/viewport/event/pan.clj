(ns top.kzre.krro.plugin.modeling.core.viewport.event.pan
  "视口平移（pan）的事件处理。

  只改 re-frame db 里的 :orbit/:target，不进 ProjectScene / COW、不进 undo、
  不产生指令。计算全部委托 ViewportMath；本命名空间只做解构、坐标差、
  调 Java、回写。

  语义：内容跟随鼠标（抓住世界拖动），相机沿反方向平移。
  :camera-view 模式下不响应（与 orbit 一致）。

  瞬态策略：绝对式。press 记录起点屏幕坐标 + 起始 target；drag 基于起点
  重算总位移，不从上一帧累加——鼠标事件丢帧时仍然正确。

  pan 用绝对式而 orbit 用增量式，因为 pan 不改变 flipped?（不越过极点），
  绝对式的起始 target 始终有效；orbit 的 flipped? 会在拖拽中 toggle，
  必须增量式。数据特性不同，不必强求一致。

  flipped? 的影响：panDelta 用 right/up 算世界位移，viewMat4 在 flipped?
  时翻转 right/up，所以 panDelta 也必须传 flipped?，否则 flip 状态下
  pan 方向会反。pan 本身不改变 flipped?，从当前视口状态读即可。

  前提：
    - frame 是带视口尺寸的上下文，由输入层提供；
    - viewport/set-frame-viewport 负责写 db、重算 viewProj、推送给 Session。"
  (:require
    [top.kzre.krro.core.reframe.core :as rf]
    [top.kzre.krro.plugin.modeling.core.project.scene :as scene]
    [top.kzre.krro.plugin.painting.core.edit.tool :as tool :refer [cleanup-tool-interceptor]]
    [top.kzre.krro.plugin.modeling.core.viewport.frame :as frame])
  (:import
    [top.kzre.krro.plugin.modeling.core.viewport ViewportMath]))

;; 瞬态工具状态。start-target 是 press 时的 target，drag 从它重算，
;; 不是从上一次 drag 的结果累加——鼠标事件丢帧时仍然正确。
(defrecord PanToolData [start-x start-y start-target]
  tool/IToolData
  (cleanup! [_ _ctx] nil)
  (overlay [_ _ctx] nil)
  (dispatch-event [_ _event-map _cofx] nil))

(defn- make-state []
  (map->PanToolData {}))

(rf/reg-event-fx
  scene/app-id :viewport-pan/press
  [(cleanup-tool-interceptor PanToolData) :factory (fn [_] (make-state))]
  (fn [cofx [_ _scene-id event-map frame]]
    (let [record (:record cofx)
          vp     (frame/get-viewport frame)
          target (get-in vp [:orbit :target])
          {:keys [x y]} event-map]
      ;; 只记录起点与起始 target，不改视口状态。
      {:record (assoc record (tool/tool-date-key)
                             (->PanToolData x y target))})))

(rf/reg-event-fx
  scene/app-id :viewport-pan/drag
  [(cleanup-tool-interceptor PanToolData)]
  (fn [cofx [_ _scene-id event-map frame
             ;; 再后续的参数整合成map传递
             {:keys [view-height]}]]
    (let [record (:record cofx)
          td     (get record (tool/tool-date-key))]
      (if (nil? td)
        {}                                   ; 非拖拽状态，忽略 stray 事件
        (let [vp (frame/get-viewport frame)]
          (if (not= :free-orbit (:mode vp))
            {}                               ; camera-view 下不响应 pan
            (let [{:keys [x y]} event-map
                  dx (- x (:start-x td))     ; 相对 press 起点的总位移
                  dy (- y (:start-y td))
                  h  (int view-height)
                  wpp (case (:projection vp)
                        :perspective
                        (ViewportMath/perspectiveWorldPerPixel
                          (double (get-in vp [:orbit :distance]))
                          (double (get-in vp [:perspective :fov-y]))
                          h)
                        :orthographic
                        (ViewportMath/orthographicWorldPerPixel
                          (double (get-in vp [:orthographic :ortho-scale]))
                          h))
                  delta (float-array 3)]
              ;; 屏幕位移 → 世界位移（内容跟随鼠标）。
              ;; flipped? 必须传：viewMat4 在 flip 时翻转了 right/up，
              ;; panDelta 不跟着翻，flip 状态下 pan 方向会反。
              (ViewportMath/panDelta
                (double (get-in vp [:orbit :azimuth]))
                (double (get-in vp [:orbit :elevation]))
                (boolean (get-in vp [:orbit :flipped?]))
                (double dx) (double dy) wpp
                delta)
              ;; 新 target = 起始 target + 世界位移，回写视口
              (let [[tx ty tz] (:start-target td)
                    target [(+ tx (aget delta 0))
                            (+ ty (aget delta 1))
                            (+ tz (aget delta 2))]
                    vp'    (assoc-in vp [:orbit :target] target)]
                {:fx [[:viewport/set-frame-viewport frame vp']]}))))))))

(rf/reg-event-fx
  scene/app-id :viewport-pan/release
  (fn [cofx [_ _scene-id _event-map _frame]]
    nil))