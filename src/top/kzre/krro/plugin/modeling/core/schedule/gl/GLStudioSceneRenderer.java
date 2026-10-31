package top.kzre.krro.plugin.modeling.core.schedule.gl;

import top.kzre.krro.canvas.gl.resource.PixelFormat;
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
import top.kzre.krro.util.math.KMath;
import top.kzre.krro.util.tile.TiledCanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class GLStudioSceneRenderer implements SceneRenderer {
    // 渲染器持有 flyweight, 调度VBO，GC 用pack GC 管理
    private final GLRenderFlyweight flyweight;
    ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>
            cornerPositions = Geometry.cornerPositions();

    /**
     * 全局共享一个 GL 计算享元
     */
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
        return Collections.singleton(cornerPositions);
    }

    @Override
    public void render(Session session) {
        GLStudioRendererScheduleSession studioSession = (GLStudioRendererScheduleSession) session;
        GLRenderer renderer = studioSession.getRenderer();

        ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> cornerPositions = this.cornerPositions.value();
        List<RenderableMesh> meshes = prepare(cornerPositions);
        CompletableFuture<TiledCanvas> future = renderer.render(RenderRequest.builder()
                .objects(meshes)
                .viewHeight(studioSession.getHeight())
                .viewWidth(studioSession.getWidth())
                .viewMat4(KMath.mat4Identity())
                .build()
        );
        future.handle((v, e)->{
            // 释放捕获资源
            cornerPositions.close();
            if (e == null) {
                // 发送结果, 因为GL 是单线程排队的，所以这里一定有序的。
                studioSession.sendView(v);
            }else {
                // 执行失败
                System.err.println(e.getMessage());
            }

            return v;
        });
    }

    private List<RenderableMesh> prepare(
            ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> cornerPositions) {
        List<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> list =
                cornerPositions.items().getSnapshot().getList();
        List<RenderableMesh> result = new ArrayList<>();
        int index = 0;
        for (CopyOnWriteObject<ListResource<CopyOnWriteFloats>> c : list) {
            if (c == null) continue;
            result.add(new RenderableMesh(
                    new DefaultCopyOnWriteObject<>(new ListResource<>(c.getSnapshot().getList())),
                    mockModelMatrix(index++)   // ← 换成非 identity
            ));
        }
        return result;
    }

    /**
     * mock 模型矩阵——列主序 float[16]。
     *
     * <p>绕 Y 轴旋转 {@code 30° * index}，缩放 1.5，沿 X 平移 {@code index * 3}，
     * Z 方向拉到 {@code -5}（相机在 {@code (0, 0, -5.5)} 看向 -Z）。
     * 正式版从场景节点的 transform 来。
     */
    private static float[] mockModelMatrix(int index) {
        double angle = Math.toRadians(30.0 * index);
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        float scale = 1.5f;
        float tx = index * 3.0f;
        float tz = -5.0f;

        // 列主序：第 0 列 = (scale*c, 0, scale*s, 0)
        //         第 2 列 = (-scale*s, 0, scale*c, 0)
        //         第 3 列 = (tx, 0, tz, 1)
        return new float[] {
                scale * c,  0f,        scale * s,  0f,
                0f,         scale,     0f,         0f,
                -scale * s,  0f,        scale * c,  0f,
                tx,         0f,        tz,         1f
        };
    }
}
