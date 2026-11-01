package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.plugin.modeling.core.schedule.SceneScheduler;

import java.util.function.Consumer;

public final class K3DRuntime {

    private K3DRuntime(){}

    private static volatile K3DRuntimeBase impl;

    /**
     * 一次性初始化。重复调用抛 IllegalStateException。
     * 由启动流程调用一次，之后不可替换。
     */
    public static synchronized void init(K3DRuntimeBase base) {
        if (base == null) throw new NullPointerException("base is null");
        if (impl != null) throw new IllegalStateException("K3DRuntime already initialized");
        impl = base;
        Runtime.getRuntime()
                .addShutdownHook(new Thread(K3DRuntime::close,
                        "k3d-runtime-shutdown"));
    }

    /** 关闭当前实现。幂等。关闭后所有静态方法抛 IllegalStateException。 */
    private static synchronized void close() {
        K3DRuntimeBase base = impl;
        if (base != null) base.close();
        // 不置 null：进程级单例，关闭即终态，不可重建
    }

    private static K3DRuntimeBase require() {
        K3DRuntimeBase base = impl;
        if (base == null) throw new IllegalStateException("K3DRuntime not initialized");
        return base;
    }

    public static SceneScheduler getSceneScheduler(long sceneId) {
        return require().getSceneScheduler(sceneId);
    }

    public static void readAssetLib(Consumer<ProjectAssetLib> c) throws Exception {
        require().readAssetLib(c);
    }

    public static void writeAssetLib(Consumer<ProjectAssetLib> c) throws Exception {
        require().writeAssetLib(c);
    }

    public static void createScene(long sceneId) {
        require().createScene(sceneId);
    }

    public static void destroyScene(long sceneId) {
        require().destroyScene(sceneId);
    }

    public static void readScene(long sceneId, Consumer<ProjectScene> c) throws Exception {
        require().readScene(sceneId, c);
    }

    public static void writeScene(long sceneId, Consumer<ProjectScene> c) throws Exception {
        require().writeScene(sceneId, c);
    }
}
