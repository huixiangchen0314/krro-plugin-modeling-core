package top.kzre.krro.plugin.modeling.core.schedule.gl;


import top.kzre.krro.d3.renderer.gl.GLRenderFlyweight;
import top.kzre.krro.d3.renderer.gl.GLStudioRenderContext;
import top.kzre.krro.plugin.modeling.core.object.IActiveObjectHolder;
import top.kzre.krro.plugin.modeling.core.object.IActiveObjectHolderFactory;
import top.kzre.krro.plugin.modeling.core.schedule.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class GLSceneSchedulerHolderFactory implements SceneSchedulerHolderFactory {
    private final GLRenderFlyweight flyweight;
    private final Function<GLStudioRenderContext, GLStudioPipeline> makePipeline;
    private final IActiveObjectHolderFactory activeObjectHolderFactory;
    private GLSceneSchedulerHolderFactory(GLRenderFlyweight flyweight,
                                          Function<GLStudioRenderContext,
                                                 GLStudioPipeline> makePipeline,
                                          IActiveObjectHolderFactory activeObjectHolderFactory) {
        this.flyweight = flyweight;
        this.makePipeline = makePipeline;
        this.activeObjectHolderFactory = activeObjectHolderFactory;
    }

    @Override
    public SceneSchedulerHolder create(long sceneId) {
        GLStudioSceneRenderer studioRenderer = new GLStudioSceneRenderer(flyweight, makePipeline);
        Map<String, SceneRenderer> rendererMap = new HashMap<>();
        rendererMap.put(RenderIds.STUDIO, studioRenderer);
        IActiveObjectHolder activeObjectHolder = activeObjectHolderFactory.create(sceneId);
        GLSceneScheduler scheduler = new GLSceneScheduler(
                rendererMap,
                sceneId,
                activeObjectHolder);
        return new SceneAnimator(scheduler, 16_666_667L);
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GLRenderFlyweight flyweight;
        private Function<GLStudioRenderContext, GLStudioPipeline> makePipeline;
        private IActiveObjectHolderFactory activeObjectHolderFactory;

        public Builder flyweight(GLRenderFlyweight flyweight) {
            this.flyweight = flyweight;
            return this;
        }

        public Builder makePipeline(Function<GLStudioRenderContext, GLStudioPipeline> makePipeline) {
            this.makePipeline = makePipeline;
            return this;
        }

        public Builder activeObjectHolderFactory(IActiveObjectHolderFactory activeObjectHolderFactory) {
            this.activeObjectHolderFactory = activeObjectHolderFactory;
            return this;
        }

        public GLSceneSchedulerHolderFactory build() {
            if (flyweight == null) {
                throw new IllegalStateException("flyweight must not be null");
            }
            if (makePipeline == null) {
                throw new IllegalStateException("makePipeline must not be null");
            }
            if (activeObjectHolderFactory == null) {
                throw new IllegalStateException("activeObjectHolderFactory must not be null");
            }

            GLSceneSchedulerHolderFactory factory = new GLSceneSchedulerHolderFactory(
                    flyweight,
                    makePipeline,
                    activeObjectHolderFactory);

            this.flyweight = null;
            this.makePipeline = null;
            this.activeObjectHolderFactory = null;

            return factory;
        }
    }
}
