(ns top.kzre.krro.plugin.modeling.core.viewport.math
  "视口状态 → 矩阵的入口。检查与解构在 Clojure，数值计算委托 ViewportMath。
   仅支持 :free-orbit；:camera-view 直接抛异常。"
  (:import [top.kzre.krro.plugin.modeling.core.viewport ViewportMath]))

(defn viewport->view-mat4
  "视口状态 → 视图矩阵，写入 mat4（float[16]，列主序）。"
  [viewport mat4]
  (when-not (= :free-orbit (:mode viewport))
    (throw (ex-info "viewport->view-mat4 only supports :free-orbit"
                    {:mode (:mode viewport)})))
  (let [{:keys [target distance azimuth elevation]} (:orbit viewport)
        [tx ty tz] target]
    (ViewportMath/viewMat4
      (double tx) (double ty) (double tz)
      (double distance) (double azimuth) (double elevation)
      ^floats mat4)
    mat4))

(defn viewport->proj-mat4
  "视口状态 → 投影矩阵，写入 mat4（float[16]，列主序）。
   需要 :aspect（宽高比），缺失抛异常。"
  [viewport mat4]
  (when-not (= :free-orbit (:mode viewport))
    (throw (ex-info "viewport->proj-mat4 only supports :free-orbit"
                    {:mode (:mode viewport)})))
  (let [aspect (double (or (:aspect viewport)
                           (throw (ex-info "viewport missing :aspect"
                                           {:viewport viewport}))))]
    (case (:projection viewport)
      :perspective
      (let [{:keys [fov-y near far]} (:perspective viewport)]
        (ViewportMath/perspectiveProjMat4
          (double fov-y) (double near) (double far) aspect ^floats mat4))
      :orthographic
      (let [{:keys [ortho-scale near far]} (:orthographic viewport)]
        (ViewportMath/orthographicProjMat4
          (double ortho-scale) (double near) (double far) aspect ^floats mat4)))
    mat4))