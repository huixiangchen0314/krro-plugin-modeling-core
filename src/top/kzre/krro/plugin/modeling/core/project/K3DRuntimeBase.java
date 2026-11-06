package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.canvas.gl.util.ResourceUtils;
import top.kzre.krro.d3.core.util.cow.COWChannelHolder;
import top.kzre.krro.plugin.modeling.core.project.accel.MeshAccelFactory;
import top.kzre.krro.plugin.modeling.core.schedule.SceneSchedulerHolderFactory;
import top.kzre.krro.plugin.modeling.core.schedule.SceneScheduler;
import top.kzre.krro.plugin.modeling.core.schedule.SceneSchedulerHolder;
import top.kzre.krro.plugin.modeling.core.util.TriFunction;
import top.kzre.krro.util.arena.Arena;
import top.kzre.krro.util.arena.ArenaTemplate;
import top.kzre.krro.util.arena.FloatArenaView;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 项目核心运行时状态，一个全局资产库 + 多个场景。
 *
 * <p><b>注意！！！</b>Arena 不是并发安全的</p>
 *
 * <p><b>场景资源打包</b>：每个场景的 {@link ProjectScene}、{@link Arena}、
 * {@link SceneSchedulerHolder} 打包在 {@link SceneBundle} 里，生命周期绑定——
 * {@link #createScene} 创建场景与 Arena，{@link #destroyScene} 统一关闭。
 * 调度器<b>懒创建</b>：首次 {@link #getSceneScheduler} 时建，与场景同生命周期关闭。</p>
 */
public abstract class K3DRuntimeBase {

    private final SceneSchedulerHolderFactory animatorFactory;
    private final COWChannelHolder<ProjectAssetLib> assetLib;

    /** sceneId → 场景资源包。键存在当且仅当该场景已创建。 */
    private final ConcurrentHashMap<Long, SceneBundle> scenes;
    private final ArenaTemplate<?, FloatArenaView> floatArenaTemplate;

    protected K3DRuntimeBase(SceneSchedulerHolderFactory schedulerFactory,
                             MeshAccelFactory makeFaceBvh,
                             ArenaTemplate<?, FloatArenaView> floatArenaTemplate) {
        this.animatorFactory = schedulerFactory;
        assetLib = COWChannelHolder.create(ProjectAssetLib.create(makeFaceBvh));
        this.floatArenaTemplate = floatArenaTemplate;
        scenes = new ConcurrentHashMap<>();

    }

    /**
     * 获取场景调度器。场景不存在时抛异常。
     *
     * <p><b>懒创建</b>：首次调用时为本场景创建 {@link SceneSchedulerHolder}
     * 并回填到 {@link SceneBundle}，之后复用。调度器的创建与关闭都归属
     * 该场景的 bundle，不单独维护第二张 map。
     *
     * <p><b>并发</b>：同场景多线程首次调用时，{@link SceneBundle} 内部
     * 用 {@code synchronized} 保证只创建一个 holder。
     */
    public SceneScheduler getSceneScheduler(long sceneId) {
        SceneBundle bundle = scenes.get(sceneId);
        if (bundle == null) {
            throw new IllegalArgumentException("Scene " + sceneId + " not found");
        }
        return bundle.getOrCreateSchedulerHolder(animatorFactory).getSceneScheduler();
    }

    /**
     * 读取资产库。
     */
    public void readAssetLib(Consumer<ProjectAssetLib> consumer) throws Exception {
        assetLib.read(consumer);
    }

    /**
     * 写入资产库。
     */
    public void writeAssetLib(Consumer<ProjectAssetLib> consumer) throws Exception {
        assetLib.write(consumer);
    }

    /**
     * 创建场景。
     *
     * <p><b>fail-fast</b>：sceneId 已存在直接抛 {@link IllegalStateException}，
     * 不覆盖、不复用——调用方 bug 应该立刻暴露，而不是静默拿到旧场景。
     *
     * <p>场景与 Arena 一起创建并打包进 {@link SceneBundle}。调度器不在此创建，
     * 由 {@link #getSceneScheduler} 懒建。创建失败时已创建的资源会被关闭，不泄漏。
     *
     * <p><b>并发</b>：两个线程同时创建同一 sceneId 时，后到者关闭自己
     * 创建的资源并抛异常，先到者的 bundle 保持有效。
     */
    public void createScene(long sceneId) {
        if (scenes.containsKey(sceneId)) {
            throw new IllegalStateException("Scene " + sceneId + " already exists");
        }

        COWChannelHolder<ProjectScene> sceneChannel;
        sceneChannel = COWChannelHolder.create(ProjectScene.create(sceneId, floatArenaTemplate));

        SceneBundle bundle = new SceneBundle(sceneId, sceneChannel);
        SceneBundle existing = scenes.putIfAbsent(sceneId, bundle);
        if (existing != null) {
            // 并发创建：另一线程先成功，关闭本次创建的资源
            RuntimeException ex = ResourceUtils.closeDelayError(bundle);
            if (ex != null) throw ex;
            throw new IllegalStateException("Scene " + sceneId + " already exists");
        }
    }

    /**
     * 销毁场景，关闭其全部资源（含懒创建的调度器）。
     * sceneId 不存在时静默返回。
     */
    public void destroyScene(long sceneId) {
        SceneBundle removed = scenes.remove(sceneId);
        if (removed == null) return;
        RuntimeException ex = ResourceUtils.closeDelayError(removed);
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * 读取场景。场景不存在时抛异常。
     */
    public void readScene(long sceneId, Consumer<ProjectScene> consumer) throws Exception {
        SceneBundle bundle = scenes.get(sceneId);
        if (bundle == null) {
            throw new NullPointerException("scene does not exist: " + sceneId);
        }
        bundle.scene.read(consumer);
    }

    /**
     * 写入场景。场景不存在时抛异常。
     */
    public void writeScene(long sceneId, Consumer<ProjectScene> consumer) throws Exception {
        SceneBundle bundle = scenes.get(sceneId);
        if (bundle == null) {
            throw new NullPointerException("scene does not exist: " + sceneId);
        }
        bundle.scene.write(consumer);
    }


    /**
     * 关闭全部资源。顺序：所有场景（含懒创建的调度器与 Arena）→ 资产库。
     * 每个 {@link SceneBundle#close()} 内部按 scheduler → scene → arena 关闭。
     */
    public void shutdown() {
        Throwable ex1 = ResourceUtils.closeDelayError(scenes.values());

        Throwable ex2 = null;
        try {
            assetLib.close();
        } catch (Exception e) {
            ex2 = e;
        }

        Throwable finalEx = ResourceUtils.accumulateException(ex1, ex2);
        if (finalEx != null) {
            throw ResourceUtils.wrapError(finalEx);
        }
    }

    /**
     * 场景相关资源的打包，生命周期绑定：
     * <ul>
     *   <li>{@code scene}——场景的 COW 通道，读写 {@link ProjectScene}</li>
     *   <li>{@code floatArena}——本场景浮点数据的分配池</li>
     *   <li>{@code schedulerHolder}——本场景的调度器持有者，<b>懒创建</b>，
     *       首次 {@link #getOrCreateSchedulerHolder} 时建，可能为 null</li>
     * </ul>
     *
     * <p>关闭顺序固定为 scheduler → scene → arena，与依赖方向相反
     * （先关消费者，后关被依赖者）。{@code schedulerHolder} 为 null
     * 表示从未取过调度器，跳过即可。
     */
    private static final class SceneBundle implements AutoCloseable {
        private final long sceneId;
        private final COWChannelHolder<ProjectScene> scene;

        /** 懒创建；volatile + synchronized 双重保证只建一次且对读线程可见。 */
        private volatile SceneSchedulerHolder schedulerHolder;

        private SceneBundle(long sceneId, COWChannelHolder<ProjectScene> scene) {
            this.sceneId = sceneId;
            this.scene = scene;
        }

        private SceneSchedulerHolder getOrCreateSchedulerHolder(
                SceneSchedulerHolderFactory factory) {
            SceneSchedulerHolder h = schedulerHolder;
            if (h != null) return h;
            synchronized (this) {
                h = schedulerHolder;
                if (h == null) {
                    h = factory.create(sceneId);
                    schedulerHolder = h;
                }
                return h;
            }
        }

        @Override
        public void close() {
            RuntimeException ex = ResourceUtils.closeDelayError(schedulerHolder, scene);
            if (ex != null) {
                throw ex;
            }
        }
    }
}