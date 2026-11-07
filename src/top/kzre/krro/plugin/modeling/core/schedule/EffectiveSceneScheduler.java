package top.kzre.krro.plugin.modeling.core.schedule;

import top.kzre.krro.d3.core.asset.MeshAsset;
import top.kzre.krro.d3.core.geometry.ExtendedMesh;
import top.kzre.krro.d3.core.scene.AssetMeshRef;
import top.kzre.krro.d3.core.scene.Hierarchy;
import top.kzre.krro.d3.core.scene.SceneObject;
import top.kzre.krro.d3.core.util.LongObjectMap;
import top.kzre.krro.d3.core.util.cow.*;
import top.kzre.krro.plugin.modeling.core.object.IActiveObjectHolder;
import top.kzre.krro.plugin.modeling.core.project.K3DRuntime;
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
    ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>>>
            TRIANGLE_INDICES = Geometry.triangleIndices();
    private final IActiveObjectHolder activeObjectHolder;
    public EffectiveSceneScheduler(
            Map<String, SceneRenderer> renderers,
            long sceneId,
            IActiveObjectHolder activeObjectHolder) {
        super(renderers, sceneId);
        this.activeObjectHolder = activeObjectHolder;
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
        Collection<Session> finalSessions = new ArrayList<>();
        for (Session session : sessions) {
            if (session.isValid()) {
                finalSessions.add(session);
            }
        }

        SceneRenderContext ctx =  SceneRenderContext.builder()
                .activeObjectId(activeObjectHolder.activeObject())
                .build();

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
        try {
            K3DRuntime.readAssetLib(assetLib->{
                try {
                    K3DRuntime.readScene(sceneId, scene->{
                        ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>
                                cornerPositions
                                = ResourceTable.create();
                        ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>>
                                triangleIndices
                                = ResourceTable.create();
                        Iterator<LongObjectMap<SceneObject>.MapEntry> objIt = scene.objects().iterator();
                        while (objIt.hasNext()) {
                            LongObjectMap<SceneObject>.MapEntry entry = objIt.next();
                            long objId = entry.getKey();
                            SceneObject obj = entry.getValue();

                            // 物体有网格引用
                            if (obj instanceof AssetMeshRef){
                                AssetMeshRef meshRef = (AssetMeshRef) obj;
                                long meshAssetId = meshRef.assetId();
                                MeshAsset meshAsset = assetLib.getMeshAsset(meshAssetId);
                                // 送去渲染
                                ExtendedMesh mesh = meshAsset.mesh();
                                if (resourceSet.contains(CORNER_POSITIONS)){
                                    // 被声明的资源
                                    CopyOnWriteFloats cps = mesh.cornerPositions().shared();
                                    DefaultCopyOnWriteObject<ListResource<CopyOnWriteFloats>> cornerPos
                                            = new DefaultCopyOnWriteObject<>(
                                            new ListResource<>(
                                                    Collections.singletonList(cps)));
                                    ResourceTables.add(cornerPositions, objId, cornerPos);
                                }
                                if (resourceSet.contains(TRIANGLE_INDICES)){
                                    CopyOnWriteInts data = mesh.triangleIndices();
//                                    System.out.println("triangle indices: " + data);
                                    CopyOnWriteInts indices = data.shared();
                                    DefaultCopyOnWriteObject<ListResource<CopyOnWriteInts>> idxes
                                            = new DefaultCopyOnWriteObject<>(
                                            new ListResource<>(
                                                    Collections.singletonList(indices)));
                                    ResourceTables.add(triangleIndices, objId, idxes);
                                }
                            }
                        }
                        CORNER_POSITIONS.value(cornerPositions);
                        TRIANGLE_INDICES.value(triangleIndices);

                        // 向渲染器注入资源
                        renderResources.forEach((s, resourceKeys) -> {
                            resourceKeys.forEach(k->{
                                if (CORNER_POSITIONS.equals(k)){
                                    ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>
                                            k1 = (ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>) k;
                                    k1.value(CORNER_POSITIONS.value().shared());
                                }
                                if (TRIANGLE_INDICES.equals(k)){
                                    ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>>>
                                            k1 = (ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>>>) k;
                                    k1.value(TRIANGLE_INDICES.value().shared());
                                }
                            });
                        });

                        // 传递给渲染器后，调度器资源立刻释放
                        CORNER_POSITIONS.doIfPresent(ResourceTable::close);
                        TRIANGLE_INDICES.doIfPresent(ResourceTable::close);
                        for (Session session : finalSessions) {
                            String renderId = session.getRenderId();
                            SceneRenderer renderer = renderers.get(renderId);
                            // 渲染器渲染视口会话
                            renderer.render(session, ctx);
                        }
                    });
                    // 渲染时候出错，日志，并不产出任何异常
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    System.err.println(Arrays.toString(e.getStackTrace()));
                }

            });
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
