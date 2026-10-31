(ns top.kzre.krro.plugin.modeling.core.project.asset
  "资产，资产库"
  (:import
    (top.kzre.krro.d3.core.asset DefaultMeshAsset)
    (top.kzre.krro.d3.core.geometry ExtendedMesh Meshes)
    (top.kzre.krro.d3.core.util.cow ResourceTables)
    (top.kzre.krro.plugin.modeling.core.project ProjectAssetLib)
    [top.kzre.krro.util.arena Arenas AutoGrowFloatArrayArenaTemplate AutoGrowIntArrayArenaTemplate]))

(defonce ^:private asset-lib*
         (ProjectAssetLib/create))

(defn asset-lb
  "获取项目资产库"
 ^ProjectAssetLib [] asset-lib*)

;; 全局 Arena
(defonce ^:private float-arena (Arenas/newEmptyFloatArray))
(defonce ^:private int-arena (Arenas/newEmptyIntArray))

(defn int-arena-template []
  (AutoGrowIntArrayArenaTemplate/fixedSize
    int-arena (* 1024 1024)))

(defn float-arena-template []
  (AutoGrowFloatArrayArenaTemplate/fixedSize
    float-arena (* 1024 1024)))

(defonce ^:private next-asset-id (atom 0))

(defn add-cube
  "往项目资产库加入一个 fake 单位 cube，返回分配的 assetId。"
  []
  (let [^ProjectAssetLib lib (asset-lb)
        meshes-w            (.meshes lib)
        f-tpl               (float-arena-template)
        i-tpl               (int-arena-template)
        ;; Mesh：8 顶点 + 24 面角 + 12 边，面角模型
        mesh                (Meshes/cube f-tpl i-tpl)
        ;; ExtendedMesh：拥有 Mesh + 派生缓存，平滑角 30°
        ext                 (ExtendedMesh. mesh f-tpl i-tpl
                                           (float (/ Math/PI 6)))
        ;; 分配 assetId——当前用进程级计数器，正式应由项目级 IdAllocator 分配
        asset-id            (long (swap! next-asset-id inc))
        ;; 包装成 MeshAsset（无形态键）
        asset               (DefaultMeshAsset/create asset-id ext nil)]
    ;; 所有权转移给表——不再持有 asset 引用
    (ResourceTables/add meshes-w asset-id asset)
    asset-id))