package top.kzre.krro.plugin.modeling.core.schedule;

import top.kzre.krro.d3.renderer.studio.gl.GLRenderFlyweight;
import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;
import top.kzre.krro.plugin.modeling.core.project.ProjectScene;
import top.kzre.krro.plugin.modeling.core.schedule.gl.GLSceneScheduler;
import top.kzre.krro.plugin.modeling.core.schedule.gl.GLStudioSceneRenderer;

import java.util.HashMap;
import java.util.Map;

public final class SceneSchedulers {

    private SceneSchedulers() {}

    public static SceneScheduler createGL(
            ProjectAssetLib assetLib,
            ProjectScene scene,
            GLRenderFlyweight flyweight) {
        GLStudioSceneRenderer studioRenderer = new GLStudioSceneRenderer(flyweight);
        Map<String, SceneRenderer> rendererMap = new HashMap<>();
        rendererMap.put(RenderIds.STUDIO, studioRenderer);

        return new GLSceneScheduler(
                rendererMap,
                assetLib,
                scene);
    }
}
