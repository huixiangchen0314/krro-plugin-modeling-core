(ns top.kzre.krro.plugin.modeling.core.viewport.event.dolly
  "视口缩放（dolly / zoom）的事件处理。

  按当前投影模式改不同字段：
    - 透视：改 :orbit/:distance（相机沿视线前后移动）
    - 正交：改 :orthographic/:ortho-scale（视野半高）
  target、azimuth、elevation、flipped? 全部不变。不进 ProjectScene / COW、
  不进 undo、不产生指令。计算委托 ViewportMath.dollyScaled。

  :camera-view 模式下不响应（与 pan / orbit 一致）。

  输入：
    :viewport-dolly/wheel   —— 滚轮，单事件。wheel-delta 约定向上滚为正。
    :viewport-dolly/press   —— Ctrl+中键拖拽开始，记录起点与起始缩放值。
    :viewport-dolly/drag    —— 拖拽中，按纵向总位移缩放。
    :viewport-dolly/release —— 清理瞬态。

  瞬态策略：绝对式。press 记录 start-y 与起始缩放值，drag 用
  (current.y - start.y) 的总位移重算，不从上一帧累加。dolly 不改变
  flipped?，绝对式不存在 orbit 那个翻越点问题。

  灵敏度：
    - 滚轮：固定 wheel-k（0.1），每格放大 e^0.1 ≈ 1.105 倍。
    - 拖拽：拖一个视口高度 = 2 倍缩放，k = ln(2) / view-height。

  钳制：distance 和 ortho-scale 都不能小于 min-value，防止矩阵奇异。
  到达下限后继续向放大方向拖/滚无效，往回拖立即响应（绝对式无死区）。

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

(def ^:private min-value 1e-3)
(def ^:private wheel-k 0.1)
(def ^:private drag-scale-per-height 4.0)
;; 瞬态工具状态。start-y 与 start-value（distance 或 ortho-scale）是 press
;; 时的快照，drag 从它们重算总缩放——绝对式，鼠标丢帧时仍然正确。
;; start-projection 记录 press 时的投影模式：拖拽中切换投影是异常情况，
;; 用 press 时的模式决定改哪个字段，避免拖拽中途字段切换。
(defrecord DollyToolData [start-y start-value start-projection]
  tool/IToolData
  (cleanup! [_ _ctx] nil)
  (overlay [_ _ctx] nil)
  (dispatch-event [_ _event-map _cofx] nil))

(defn- make-state []
  (map->DollyToolData {}))

(defn- current-scale-value
  "按投影模式返回当前的缩放源值（distance 或 ortho-scale）。"
  [vp projection]
  (case projection
    :perspective  (get-in vp [:orbit :distance])
    :orthographic (get-in vp [:orthographic :ortho-scale])))

(defn- assoc-scale-value
  "按投影模式把缩放结果写回视口。"
  [vp projection v]
  (case projection
    :perspective  (assoc-in vp [:orbit :distance] v)
    :orthographic (assoc-in vp [:orthographic :ortho-scale] v)))

;; ---------------------------------------------------------------------------
;; wheel —— 滚轮，单事件
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  scene/app-id :viewport-dolly/wheel
  (fn [_cofx [_ _scene-id event-map frame]]
    (let [vp (frame/get-viewport frame)]
      (if (not= :free-orbit (:mode vp))
        {}                                   ; camera-view 下不响应
        (let [{:keys [delta-y]} event-map
              proj (:projection vp)
              old  (double (current-scale-value vp proj))
              ;; delta 语义：正 = 放大。delta-y 约定向上滚为正。
              v'   (ViewportMath/dollyScaled
                     old (double delta-y) wheel-k (double min-value))]
          {:fx [[:viewport/set-frame-viewport frame
                 (assoc-scale-value vp proj v')]]})))))

;; ---------------------------------------------------------------------------
;; press / drag / release —— 拖拽缩放
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  scene/app-id :viewport-dolly/press
  [(cleanup-tool-interceptor DollyToolData) :factory (fn [_] (make-state))]
  (fn [cofx [_ _scene-id event-map frame]]
    (let [record (:record cofx)
          vp     (frame/get-viewport frame)
          proj   (:projection vp)
          {:keys [y]} event-map]
      ;; 只记录起点与起始缩放值，不改视口状态。
      {:record (assoc record (tool/tool-date-key)
                             (->DollyToolData y
                                              (current-scale-value vp proj)
                                              proj))})))

(rf/reg-event-fx
  scene/app-id :viewport-dolly/drag
  [(cleanup-tool-interceptor DollyToolData)]
  (fn [cofx [_ _scene-id event-map frame
             {:keys [view-height]}]]
    (let [record (:record cofx)
          td     (get record (tool/tool-date-key))
          h      (long (or view-height 0))]
      (if (or (nil? td) (not (pos? h)))
        {}                                   ; 非拖拽状态或视口未就绪，忽略
        (let [vp (frame/get-viewport frame)]
          (if (not= :free-orbit (:mode vp))
            {}                               ; camera-view 下不响应
            (let [{:keys [y]} event-map
                  total-dy (- (double y) (double (:start-y td)))
                  ;; delta = -total-dy：向上拖 total-dy<0，delta>0 = 放大
                  delta    (- total-dy)
                  ;; 一个视口高度 = drag-scale-per-height 倍
                  k        (/ (Math/log drag-scale-per-height) (double h))
                  proj     (:start-projection td)
                  v'       (ViewportMath/dollyScaled
                             (double (:start-value td)) delta k (double min-value))
                  vp'      (assoc-scale-value vp proj v')]
              {:fx [[:viewport/set-frame-viewport frame vp']]})))))))

(rf/reg-event-fx
  scene/app-id :viewport-dolly/release
  (fn [_cofx _]
    ;; 清理由 cleanup-tool-interceptor 负责。与 pan / orbit 一致。
    nil))