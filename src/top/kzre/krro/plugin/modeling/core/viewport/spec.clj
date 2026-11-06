(ns top.kzre.krro.plugin.modeling.core.viewport.spec
  "3D 视口状态的数据定义。

   视口状态是纯数据，用 map 承载，不做 record 封装——字段开放、assoc/update-in
   自由、EDN 可直接序列化。本命名空间只声明数据结构（spec），不包含任何行为；
   视图矩阵的推导在独立的纯函数命名空间（如 viewport.math）中实现。

   一个视口状态包含三块，互相独立：
     - :mode       —— 观察方式：自由轨道 / 相机画面
     - :orbit      —— 轨道相机参数（FREE_ORBIT 模式使用；CAMERA_VIEW 时保留不丢）
     - :projection —— 投影方式 + 两套投影参数（切换时另一套保留不丢）
 "
  (:require [clojure.spec.alpha :as s])
  (:import (top.kzre.krro.plugin.modeling.core.viewport ViewportMath)))

;; ============================================================================
;; 轨道参数（FREE_ORBIT 模式）
;; ============================================================================
;; 轨道相机是球坐标模型：相机位置 = target + 球坐标(distance, azimuth, elevation)。
;; 三个角度/距离参数是用户意图的源数据，视图矩阵由纯函数推导。
;; CAMERA_VIEW 模式下这组参数不参与计算，但保留——切回 FREE_ORBIT 时恢复原视角。

(s/def ::target
  ;; 注视点（世界坐标）。相机绕此点旋转，始终朝向它。
  (s/tuple double? double? double?))

(s/def ::distance
  ;; 相机到注视点的距离。必须为正——零距离会导致视图矩阵退化。
  (s/and double? pos?))

(s/def ::azimuth
  ;; 绕世界 Y 轴的方位角，弧度，无界（周期 2π，由推导函数取模）。
  double?)

(s/def ::elevation
  ;; 仰角，弧度，范围闭区间 [-ELEVATION_LIMIT, ELEVATION_LIMIT]。
  ;; 上下界均为 π/2 - ε（留 1e-6 余量而非闭区间到 π/2），
  ;; 避免浮点边界恰好落在奇异点上。orbitAnglesClamped 把 elevation 钳制在此范围内——
  ;; 相机永不倒置，因此不需要 flipped 状态。
  (s/and double?
         #(<= (- ViewportMath/ELEVATION_LIMIT) %)
         #(<= % ViewportMath/ELEVATION_LIMIT)))

(s/def ::orbit
  ;; 轨道相机的完整参数组。三个字段缺一不可——推导视图矩阵需要全部。
  (s/keys :req-un [::target ::distance ::azimuth ::elevation]))

;; ============================================================================
;; 投影参数
;; ============================================================================
;; 透视与正交两套参数同时存在，:projection 只标记当前激活哪一套。
;; 切换投影时另一套不丢失，切回来恢复——与 Blender 行为一致。
;; 两套都要求 :near / :far，但取值范围不同（见各自注释）。

(s/def ::fov-y
  ;; 垂直视角，弧度，开区间 (0, π)。
  ;; 0 会导致投影矩阵除零；π 会导致视野覆盖整个球面，无意义。
  (s/and double? #(< 0 % Math/PI)))

(s/def ::near
  ;; 近裁剪面距离。
  ;; 透视投影下必须为正（相机前方），推导函数负责校验；
  ;; 正交投影下可为负（允许相机后方的内容可见），故此 spec 不约束符号。
  double?)

(s/def ::far
  ;; 远裁剪面距离。必须为正。near < far 的关系由推导函数校验，
  ;; 不在此处用跨字段 predicate 表达——spec 保持字段级，跨字段约束归行为层。
  (s/and double? pos?))

(s/def ::perspective
  ;; 透视投影参数组。fov-y / near / far 缺一不可。
  (s/keys :req-un [::fov-y ::near ::far]))

(s/def ::ortho-scale
  ;; 正交视野半高（世界单位）。必须是正数。
  ;; 约定：半高，不是全高。投影矩阵中 scale 直接对应 1/ortho-scale，
  ;; 与 Blender 的 ortho_scale（全高）差 2 倍，序列化/导入导出时注意换算。
  (s/and double? pos?))

(s/def ::orthographic
  ;; 正交投影参数组。ortho-scale / near / far 缺一不可。
  (s/keys :req-un [::ortho-scale ::near ::far]))

(s/def ::projection
  ;; 当前激活的投影方式。两套参数始终存在，此字段只做选择。
  #{:perspective :orthographic})

;; ============================================================================
;; 视口
;; ============================================================================

(s/def ::mode
  ;; 观察方式：
  ;;   :free-orbit  —— 轨道相机自由导航，用 :orbit 参数推导视图矩阵
  ;;   :camera-view —— 通过场景内某个相机节点观察，用该节点的世界变换
  #{:free-orbit :camera-view})

(s/def ::active-camera
  ;; 场景相机节点 id（对应 ProjectScene 中的节点）。
  ;; 仅在 :camera-view 模式下有意义；:free-orbit 时保留（切回时恢复），可为 nil。
  ;; 注意：加载项目时若该 id 在场景中不存在（相机被删），加载逻辑需回退到
  ;; :free-orbit 或默认相机——悬空引用的校验在加载层做，不在本 spec 做。
  (s/nilable int?))

(s/def ::viewport
  ;; 视口状态的完整结构。
  ;; 必填：mode / orbit / projection / perspective / orthographic。
  ;;   - orbit 在 camera-view 下也必填（保留切换前的视角）
  ;;   - perspective / orthographic 都必填（切换投影不丢参数）
  ;; 可选：active-camera。
  ;; 跨字段约束：camera-view 模式必须有 active-camera；free-orbit 不要求。
  (s/and
    (s/keys :req-un [::mode ::orbit ::projection ::perspective ::orthographic]
            :opt-un [::active-camera])
    (fn [{:keys [mode active-camera]}]
      (or (not= mode :camera-view)
          (some? active-camera)))))

;; ============================================================================
;; 默认值
;; ============================================================================

(def ^:private default-viewport*
  "新建视口的初始状态。
   - 轨道：距离 10，注视原点，方位角/仰角 0（正前方略平视）
   - 投影：透视，45° 垂直视角，near 0.1 / far 1000
   - 正交参数预置一套合理值，切换投影时直接可用
   - 无激活相机（:free-orbit 模式）"
  {:mode         :free-orbit
   :orbit        {:target    [0.0 0.0 0.0]
                  :distance  10.0
                  :azimuth   0.0
                  :elevation 0.0}
   :projection   :perspective
   :perspective  {:fov-y (/ Math/PI 4)          ; 45°
                  :near  0.1
                  :far   1000.0}
   :orthographic {:ortho-scale 10.0
                  :near       -1000.0           ; 正交 near 可为负
                  :far        1000.0}})

(defn make-viewport []
  default-viewport*)