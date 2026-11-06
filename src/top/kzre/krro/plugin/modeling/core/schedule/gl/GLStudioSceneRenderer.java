package top.kzre.krro.plugin.modeling.core.schedule.gl;

import top.kzre.krro.canvas.gl.resource.PixelFormat;
import top.kzre.krro.d3.core.util.LongIntMap;
import top.kzre.krro.d3.core.util.ResourceUtils;
import top.kzre.krro.d3.core.util.cow.*;
import top.kzre.krro.d3.renderer.studio.gl.GLRenderFlyweight;
import top.kzre.krro.d3.renderer.studio.gl.GLRenderer;
import top.kzre.krro.d3.renderer.studio.gl.GLStudioRenderContext;
import top.kzre.krro.d3.renderer.studio.pipeline.RenderRequest;
import top.kzre.krro.d3.renderer.studio.pipeline.RenderableMesh;
import top.kzre.krro.plugin.modeling.core.schedule.SceneRenderer;
import top.kzre.krro.plugin.modeling.core.schedule.ScheduleSession;
import top.kzre.krro.plugin.modeling.core.schedule.Session;
import top.kzre.krro.plugin.modeling.core.schedule.resource.Geometry;
import top.kzre.krro.plugin.modeling.core.schedule.resource.ResourceKey;
import top.kzre.krro.plugin.modeling.core.viewport.ViewportMath;
import top.kzre.krro.util.math.KMath;
import top.kzre.krro.util.tile.TiledCanvas;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class GLStudioSceneRenderer implements SceneRenderer {

    private final GLRenderFlyweight flyweight;

    private final ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>
            cornerPositionsKey = Geometry.cornerPositions();

    private final ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>>>
            triangleIndicesKey = Geometry.triangleIndices();

    public GLStudioSceneRenderer(GLRenderFlyweight flyweight) {
        this.flyweight = flyweight;
    }

    @Override
    public ScheduleSession createSession() {
        GLStudioRenderContext context = new GLStudioRenderContext(flyweight, PixelFormat.RGBA8);
        return new GLStudioRendererScheduleSession(context);
    }

    @Override
    public Set<ResourceKey<?>> getResourceKeys() {
        HashSet<ResourceKey<?>> result = new HashSet<>();
        result.add(cornerPositionsKey);
        result.add(triangleIndicesKey);
        return result;
    }

    @Override
    public void render(Session session) {
        GLStudioRendererScheduleSession studioSession = (GLStudioRendererScheduleSession) session;
        GLRenderer renderer = studioSession.getRenderer();
        float[] viewProjMat4 = studioSession.getViewProjMat4();

        // 两个表的 value() 必须成对取——prepare 需要按索引对齐
        ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> posTable =
                cornerPositionsKey.value();
        ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>> idxTable =
                triangleIndicesKey.value();

        List<RenderableMesh> meshes = prepare(posTable, idxTable);

        CompletableFuture<TiledCanvas> future = renderer.render(RenderRequest.builder()
                .objects(meshes)
                .viewHeight(studioSession.getHeight())
                .viewWidth(studioSession.getWidth())
                .viewMat4(viewProjMat4)
                .build()
        );

        future.handle((v, e) -> {
            // 释放捕获资源——两个表都要关
            try {
                RuntimeException ignore = ResourceUtils.closeDelayError(posTable, idxTable);
            } catch (RuntimeException closeEx) {
                if (e == null) {
                    e = closeEx;
                } else {
                    e.addSuppressed(closeEx);
                }
            }

            // 释放 prepare 构造的临时 COW 包装（底层 arena 由表释放，这里只清引用）
            // TODO RenderableMesh 不是COW，资源不属于它
            for (RenderableMesh mesh : meshes) {
                try {
                    mesh.close();
                } catch (RuntimeException ignored) {
                    // 渲染已完成，close 失败只影响 GC 时机，不影响正确性
                }
            }

            if (e == null) {
                studioSession.sendView(v);
            } else {
                System.err.println(e.getMessage());
            }
            return v;
        });
    }

    /**
     * 把两个资源表按索引配对为 {@link RenderableMesh} 列表。
     *
     * <p>两个表由 {@link #getResourceKeys()} 声明，调度层保证它们按
     * 同一资源 id 对齐——索引 i 处的 positions 与 indices 属于同一
     * 几何体。任一为 null 视为空洞，跳过。
     *
     * <p>用 {@code new DefaultCopyOnWriteObject<>(new ListResource<>(...))}
     * 包装 snapshot——不 shared 原始 COW：renderer 对 COW 的引用不应
     * 影响原始资源的生命周期，原始表的 close 由 {@link #render} 的
     * future 回调统一负责。
     */
    private List<RenderableMesh> prepare(
            ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> posTable,
            ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>> idxTable) {
        List<RenderableMesh> result = new ArrayList<>();

        Iterator<LongIntMap.MapEntry> iterator = posTable.indexMap().getSnapshot().getMap().iterator();
        while (iterator.hasNext()) {
            LongIntMap.MapEntry entry = iterator.next();
            long objId = entry.getKey();
            CopyOnWriteObject<ListResource<CopyOnWriteFloats>> pos = posTable.get(objId);
            CopyOnWriteObject<ListResource<CopyOnWriteInts>> idx = idxTable.get(objId);
            // 跳过空洞——任一为 null 视为该资源缺失
            if (pos == null || idx == null) {
                continue;
            }
            result.add(new RenderableMesh(
                    new DefaultCopyOnWriteObject<>(
                            new ListResource<>(pos.getSnapshot().getList())),
                    new DefaultCopyOnWriteObject<>(
                            new ListResource<>(idx.getSnapshot().getList())),
                    KMath.mat4Identity()));
        }

        return result;
    }
}