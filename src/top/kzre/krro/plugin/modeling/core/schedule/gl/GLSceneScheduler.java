package top.kzre.krro.plugin.modeling.core.schedule.gl;

import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;
import top.kzre.krro.plugin.modeling.core.project.ProjectScene;
import top.kzre.krro.plugin.modeling.core.schedule.EffectiveSceneScheduler;
import top.kzre.krro.plugin.modeling.core.schedule.SceneRenderer;

import java.util.Map;

public final class GLSceneScheduler extends EffectiveSceneScheduler {
    public GLSceneScheduler(Map<String, SceneRenderer> renderers, long sceneId) {
        super(renderers, sceneId);
    }


    // TODO 准备 GL 资源
}
