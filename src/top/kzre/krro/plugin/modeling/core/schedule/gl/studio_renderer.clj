(ns top.kzre.krro.plugin.modeling.core.schedule.gl.studio-renderer
  (:require
   [top.kzre.krro.d3.core.renderer.core :as renderer]
   [top.kzre.krro.d3.renderer.gl.studio.core :as studio]
   [top.kzre.krro.d3.renderer.pass.core :as pass])
  (:import
   [java.util.function Function]
   (top.kzre.krro.canvas.gl.util ExecutorUtils)
   (top.kzre.krro.core.util SerialExecutor)
   (top.kzre.krro.d3.renderer.gl GLStudioRenderContext)
   (top.kzre.krro.plugin.modeling.core.schedule.gl GLStudioPipeline)
   (top.kzre.krro.util.tile TiledCanvas)))

(defrecord State
  [view-proj-mat4
   view-width
   view-height
   instances])
(defn- make-state []
  (-> map->State {}))

(defrecord GLStudioPipelineImpl
  [^GLStudioRenderContext render-context
   state-atom]
  GLStudioPipeline
  (close [_]
    (.close render-context))
  (begin [this]
    ;; 重置状态
    (reset! state-atom (make-state))
    this)
  (setViewSize [this view-width view-height]
    (swap! state-atom assoc
           :view-width view-width
           :view-height view-height)
    this)
  (setViewProjMat4 [this view-proj-mat4]
    (swap! state-atom assoc
           :view-proj-mat4 view-proj-mat4)
    this)
  (setInstances [this instances]
    (swap! state-atom assoc
           :instances instances)
    this)
  (render [_]
    (let [^SerialExecutor gl-thread
          (.getGlExecutor render-context)
          basis (pass/create-basis)
          {:keys [view-width
                  view-height
                  instances
                  view-proj-mat4]} @state-atom]
      (ExecutorUtils/runOnThread
        gl-thread
        (reify Callable
          (call ^TiledCanvas [_]
            (let [{:keys [canvas]}
                  (renderer/render
                    {:renderer (studio/pipeline-id)
                     :basis basis
                     :gl-render-context render-context
                     :view-proj-mat4 view-proj-mat4
                     :view-width view-width
                     :view-height view-height
                     :instances instances})]
              ;; TODO 显示的从 GL 线程调度出来
              canvas)))))))

(defn- make-renderer
  [^GLStudioRenderContext ctx]
  (->GLStudioPipelineImpl ctx (atom (make-state))))


(defn make-renderer-func []
  (reify Function
    (apply [_ render-ctx]
      (make-renderer render-ctx))))