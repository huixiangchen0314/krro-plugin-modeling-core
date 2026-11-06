(ns top.kzre.krro.plugin.modeling.core.project.accel
  "加速结构适配"
  (:require
    [top.kzre.krro.d3.core.geometry.binned-bvh :as binned-bvh]
    [top.kzre.krro.d3.core.geometry.bvh :as bvh])
  (:import
    (java.util.function BiFunction Function)
    (top.kzre.krro.d3.core.geometry SpatialOverlay)
    (top.kzre.krro.d3.core.geometry.bvh BVH)
    (top.kzre.krro.d3.core.util IntList)
    (top.kzre.krro.plugin.modeling.core.project.accel BVHMeshAccelFactory BVHSceneAccelFactory RayAccel SpatialOverlayUtils)
    (top.kzre.krro.plugin.modeling.core.util TriFunction))
  )
(defn- fn->func [f]
  (reify Function
    (apply [_ a]
      (f a))))


(defn- fn->bi-func [f]
  (reify BiFunction
    (apply [_ a b]
      (f a b))))

(defrecord BVHAccel
  [^BVH bvh
   ^IntList acc]
  RayAccel
  (query [_ ray]
    (let [^SpatialOverlay o (bvh/cross-query bvh ray :acc acc)]
      (.toLongArray o))))

(defn- make-bvh-accel
  [^BVH bvh]
  (->BVHAccel bvh (IntList.)))

(defrecord IndexedBVHAccel
  [^BVH bvh
   ^longs index->id
   ^IntList acc]
  RayAccel
  (query [_ ray]
    (let [^SpatialOverlay o (bvh/cross-query bvh ray :acc acc)]
      (SpatialOverlayUtils/indexToId o index->id))))

(defn- make-indexed-bvh-accel
  [^BVH bvh
   ^longs index->id]
  (->IndexedBVHAccel bvh index->id (IntList.)))

(defn scene-accel-factory
  "场景加速结构工厂"
  []
  (BVHSceneAccelFactory.
    (fn->bi-func binned-bvh/build-binned-bvh)
    (fn->bi-func make-indexed-bvh-accel)))

(defn mesh-accel-factory
  "网格面加速结构工厂"
  []
  (BVHMeshAccelFactory.
    (reify TriFunction
      (apply [_ aabbs count offset]
        (binned-bvh/build-binned-bvh aabbs count :offset offset)))
    (fn->func make-bvh-accel)))
