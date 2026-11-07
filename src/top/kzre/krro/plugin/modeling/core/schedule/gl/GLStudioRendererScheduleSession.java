package top.kzre.krro.plugin.modeling.core.schedule.gl;

import top.kzre.krro.plugin.modeling.core.schedule.RenderIds;
import top.kzre.krro.plugin.modeling.core.schedule.ScheduleSession;

/**
 * GLStudio 渲染器的资源句柄
 */
public final class GLStudioRendererScheduleSession extends ScheduleSession {

    private final GLStudioPipeline pipeline;
    public GLStudioRendererScheduleSession(GLStudioPipeline pipeline) {
        this.pipeline = pipeline;
    }


    @Override
    protected void onClose() {
        try {
            pipeline.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRenderId() {
        return RenderIds.STUDIO;
    }

    public GLStudioPipeline getPipeline() {
        return pipeline;
    }
}
