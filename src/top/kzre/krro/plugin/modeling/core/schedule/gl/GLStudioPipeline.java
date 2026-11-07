package top.kzre.krro.plugin.modeling.core.schedule.gl;

import top.kzre.krro.d3.renderer.resource.RenderInstance;
import top.kzre.krro.util.tile.TiledCanvas;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工作室渲染器抽象接口，适配clojure调用
 */
public interface GLStudioPipeline extends AutoCloseable{
    GLStudioPipeline begin();
    GLStudioPipeline setViewSize(int width, int height);
    GLStudioPipeline setViewProjMat4(float[] mat4);
    GLStudioPipeline setInstances(List<RenderInstance> instances);
    GLStudioPipeline setActiveInstanceId(Long objId);
    CompletableFuture<TiledCanvas> render();
}
