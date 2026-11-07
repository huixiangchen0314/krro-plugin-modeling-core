package top.kzre.krro.plugin.modeling.core.schedule;


import top.kzre.krro.d3.renderer.gl.GLRenderFlyweight;
import top.kzre.krro.d3.renderer.gl.GLStudioRenderContext;
import top.kzre.krro.plugin.modeling.core.schedule.gl.GLSceneScheduler;
import top.kzre.krro.plugin.modeling.core.schedule.gl.GLStudioPipeline;
import top.kzre.krro.plugin.modeling.core.schedule.gl.GLStudioSceneRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class SceneSchedulers {

    private SceneSchedulers() {}

    public static GLSceneScheduler createGL(
            long sceneId,
            GLRenderFlyweight flyweight,
            Function<GLStudioRenderContext, GLStudioPipeline> makePipeline) {
        GLStudioSceneRenderer studioRenderer = new GLStudioSceneRenderer(flyweight, makePipeline);
        Map<String, SceneRenderer> rendererMap = new HashMap<>();
        rendererMap.put(RenderIds.STUDIO, studioRenderer);

        return new GLSceneScheduler(
                rendererMap,
                sceneId);
    }
}
