package top.kzre.krro.plugin.modeling.core.schedule;

/**
 * Holder 模式。环境和内部资源隔离
 */
public interface SceneSchedulerHolder extends AutoCloseable {
    SceneScheduler getSceneScheduler();
}
