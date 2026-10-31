package top.kzre.krro.plugin.modeling.core.schedule.gl;

import top.kzre.krro.d3.renderer.studio.gl.GLRenderer;
import top.kzre.krro.d3.renderer.studio.gl.GLStudioRenderContext;
import top.kzre.krro.d3.renderer.studio.pipeline.StudioPipeline;
import top.kzre.krro.plugin.modeling.core.schedule.RenderIds;
import top.kzre.krro.plugin.modeling.core.schedule.ScheduleSession;

/**
 * GLStudio 渲染器的资源句柄
 */
public final class GLStudioRendererScheduleSession extends ScheduleSession {
    private final GLStudioRenderContext renderContext;
    private final GLRenderer renderer;
    public GLStudioRendererScheduleSession(GLStudioRenderContext renderContext) {
        this.renderContext = renderContext;
        this.renderer = new GLRenderer(new StudioPipeline(), renderContext);
    }

    public GLStudioRenderContext getRenderContext() {
        return renderContext;
    }

    @Override
    protected void onClose() {
        renderContext.close();
    }

    @Override
    public String getRenderId() {
        return RenderIds.STUDIO;
    }

    public GLRenderer getRenderer() {
        return renderer;
    }
}
