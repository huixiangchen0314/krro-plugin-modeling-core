package top.kzre.krro.plugin.modeling.core.schedule;

import top.kzre.krro.plugin.modeling.core.schedule.resource.ResourceKey;
import top.kzre.krro.util.tile.TiledCanvas;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 场景渲染器
 * 1. 能渲染视图
 * 2. 能创建session
 */
public interface SceneRenderer extends SessionFactory{
    // TODO 场景渲染器配置，和渲染资源声明
    Set<ResourceKey<?>> getResourceKeys();

    void render(Session cb,SceneRenderContext ctx);
}
