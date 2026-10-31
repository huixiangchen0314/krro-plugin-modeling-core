package top.kzre.krro.plugin.modeling.core.schedule;

import top.kzre.krro.d3.core.asset.MeshAsset;
import top.kzre.krro.d3.core.geometry.ExtendedMesh;
import top.kzre.krro.d3.core.scene.AssetMeshRef;
import top.kzre.krro.d3.core.scene.Scene;
import top.kzre.krro.d3.core.util.cow.*;
import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;
import top.kzre.krro.plugin.modeling.core.project.ProjectScene;
import top.kzre.krro.plugin.modeling.core.schedule.resource.Geometry;
import top.kzre.krro.plugin.modeling.core.schedule.resource.ResourceKey;

import java.util.*;

/**
 * 场景资源调度器
 */
public abstract class EffectiveSceneScheduler extends AbstractSceneScheduler {
    private final
    ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>
            CORNER_POSITIONS = Geometry.cornerPositions();

    public EffectiveSceneScheduler(Map<String, SceneRenderer> renderers,
                                   ProjectAssetLib assetLib,
                                   ProjectScene scene) {
        super(renderers, assetLib, scene);
    }

    /**
     * 每个场景单线程调度，不并发
     */
    @SuppressWarnings("unchecked")
    @Override
    public void poll() {
        // 无 渲染会话，不执行渲染
        // 添加新的渲染会话后，必须poll一次才能接受视口，这是自然的结果
        if (renderers.isEmpty()) {
           throw new IllegalStateException("No renderers have been initialised");
        }

        if (sessions.isEmpty()) {
            return;
        }
        // 本地化 sessions
        Collection<Session> finalSessions = Collections.unmodifiableCollection(new ArrayList<>(sessions));

        // 去重资源集
        Set<ResourceKey<?>> resourceSet = new HashSet<>();
        // 每个渲染器的资源需求
        Map<String, Set<ResourceKey<?>>> renderResources = new HashMap<>();

        renderers.forEach((s, renderer) -> {
            Set<ResourceKey<?>> resourceKeys = renderer.getResourceKeys();
            renderResources.put(s, resourceKeys);
            resourceSet.addAll(resourceKeys);
        });

        // 固化版本
        // TODO 原版本上触发缓存，需要 ProjectAssetLib 做同步
        try(ProjectAssetLib finalAssetLib = assetLib.shared()) {
            try (ProjectScene finalScene = scene.shared()) {
                Scene hierachy = finalScene.getSceneEditor().scene();
                ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>
                        cornerPositions
                        = ResourceTable.create();
                // 遍历场景
                hierachy.objects().getSnapshot().getMap().forEach((objId, obj)->{
                    // 物体有网格引用
                    if (obj instanceof AssetMeshRef){
                        AssetMeshRef meshRef = (AssetMeshRef) obj;
                        long meshAssetId = meshRef.assetId();
                        MeshAsset meshAsset = finalAssetLib.getMeshAsset(meshAssetId);
                        // 送去渲染
                        ExtendedMesh mesh = meshAsset.mesh();
                        if (resourceSet.contains(CORNER_POSITIONS)){
                            // 被声明的资源
                            CopyOnWriteFloats cps = mesh.getCornerPositions().shared();
                            DefaultCopyOnWriteObject<ListResource<CopyOnWriteFloats>> cornerPos
                                    = new DefaultCopyOnWriteObject<>(
                                            new ListResource<>(
                                                    Collections.singletonList(cps)));
                            ResourceTables.add(cornerPositions, objId, cornerPos);
                        }
                    }
                });
                CORNER_POSITIONS.value(cornerPositions);

                // TODO 进阶数据计算。节点图等。

                // 向渲染器注入资源
                renderResources.forEach((s, resourceKeys) -> {
                    resourceKeys.forEach(k->{
                        if (CORNER_POSITIONS.equals(k)){
                            ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>
                                    k1 = (ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>) k;
                            k1.value(CORNER_POSITIONS.value().shared());
                        }
                    });
                });

                // 传递给渲染器后，调度器资源立刻释放
                CORNER_POSITIONS.doIfPresent(ResourceTable::close);

                for (Session session : finalSessions) {
                    String renderId = session.getRenderId();
                    SceneRenderer renderer = renderers.get(renderId);
                    // 渲染器渲染视口会话
                    renderer.render(session);
                }

            }
        }

    }
}
