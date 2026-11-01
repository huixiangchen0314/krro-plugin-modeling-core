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
import top.kzre.krro.plugin.modeling.core.viewport.ViewportMath;
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
        float[] viewProjMat4 = studioSession.getViewProjMat4();
        ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> cornerPositions = this.cornerPositions.value();
        List<RenderableMesh> meshes = prepare(cornerPositions);
        CompletableFuture<TiledCanvas> future = renderer.render(RenderRequest.builder()
                .objects(meshes)
                .viewHeight(studioSession.getHeight())
                .viewWidth(studioSession.getWidth())
                .viewMat4(viewProjMat4)
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
            ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> cornerPositions){
        List<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>> list = cornerPositions.items().getSnapshot().getList();
        List<RenderableMesh> result = new ArrayList<>();
        for (CopyOnWriteObject<ListResource<CopyOnWriteFloats>> c : list) {
            // 跳过空洞
            if (c == null) {
                continue;
            }
            // 不shared，资源不属于实际的渲染器
            result.add(new RenderableMesh(new DefaultCopyOnWriteObject<>(
                    new ListResource<>(c.getSnapshot().getList())
            ),  KMath.mat4Identity()));
        }

        return result;
    }
}
