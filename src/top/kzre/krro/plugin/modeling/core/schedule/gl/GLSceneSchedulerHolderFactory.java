package top.kzre.krro.plugin.modeling.core.schedule.gl;

import top.kzre.krro.d3.renderer.studio.gl.GLRenderFlyweight;
import top.kzre.krro.plugin.modeling.core.schedule.*;

public final class GLSceneSchedulerHolderFactory implements SceneSchedulerHolderFactory {
    private final GLRenderFlyweight flyweight;

    public GLSceneSchedulerHolderFactory(GLRenderFlyweight flyweight) {
        this.flyweight = flyweight;
    }

    @Override
    public SceneSchedulerHolder create(long sceneId) {
        GLSceneScheduler scheduler = SceneSchedulers.createGL(sceneId, flyweight);
        return new SceneAnimator(scheduler, 16_666_667L);
    }
}
