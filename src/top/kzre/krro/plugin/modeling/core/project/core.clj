(ns top.kzre.krro.plugin.modeling.core.project.core
  (:import [top.kzre.krro.plugin.modeling.core.project K3DRuntime]
           [java.util.function Consumer]))

(defn- fn->consumer
  "函数 → Consumer。reify 只做适配，类型 hint 留给 f 内部，
   避免反射散落在适配层。"
  [f]
  (reify Consumer
    (accept [_ x] (f x))))

(defn read-scene
  "在 shared 快照上读。shared 由 COWChannelHolder 在回调结束后 close，
   不要把它带出 f 的作用域。"
  [scene-id f]
  (K3DRuntime/readScene scene-id (fn->consumer f)))

(defn write-scene!
  "在 owner 上写，回调结束时一次屏障提交。"
  [scene-id f]
  (try
    (K3DRuntime/writeScene scene-id (fn->consumer f))
    (catch InterruptedException e
      (.interrupt (Thread/currentThread))
      (throw e))))

(defn read-asset-lib [f]
  (K3DRuntime/readAssetLib (fn->consumer f)))

(defn write-asset-lib! [f]
  (try
    (K3DRuntime/writeAssetLib (fn->consumer f))
    (catch InterruptedException e
      (.interrupt (Thread/currentThread))
      (throw e))))

(defn create-scene! [scene-id] (K3DRuntime/createScene scene-id))
(defn destroy-scene! [scene-id] (K3DRuntime/destroyScene scene-id))