package top.kzre.krro.plugin.modeling.core.schedule;

/**
 * 场景调度器
 */
public interface SceneScheduler {

    /**
     * 开启渲染会话，指定渲染器
     */
    Session openSession(String rendererId);

}
