package top.kzre.krro.plugin.modeling.core.schedule.gl;


import top.kzre.krro.d3.renderer.gl.GLRenderFlyweight;
import top.kzre.krro.d3.renderer.gl.GLStudioRenderContext;
import top.kzre.krro.plugin.modeling.core.schedule.*;

import java.util.function.Function;

public final class GLSceneSchedulerHolderFactory implements SceneSchedulerHolderFactory {
    private final GLRenderFlyweight flyweight;
    private final Function<GLStudioRenderContext, GLStudioPipeline> makePipeline;
    public GLSceneSchedulerHolderFactory(GLRenderFlyweight flyweight,
                                         Function<GLStudioRenderContext,
                                                 GLStudioPipeline> makePipeline) {
        this.flyweight = flyweight;
        this.makePipeline = makePipeline;
    }

    @Override
    public SceneSchedulerHolder create(long sceneId) {
        GLSceneScheduler scheduler = SceneSchedulers.createGL(sceneId, flyweight, makePipeline);
        return new SceneAnimator(scheduler, 16_666_667L);
    }
}
