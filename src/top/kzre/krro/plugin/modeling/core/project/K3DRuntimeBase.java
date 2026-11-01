package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.canvas.gl.util.ResourceUtils;
import top.kzre.krro.plugin.modeling.core.schedule.SceneSchedulerHolderFactory;
import top.kzre.krro.plugin.modeling.core.schedule.SceneScheduler;
import top.kzre.krro.plugin.modeling.core.schedule.SceneSchedulerHolder;
import top.kzre.krro.plugin.modeling.core.util.COWChannelHolder;
import top.kzre.krro.util.arena.ArenaTemplate;
import top.kzre.krro.util.arena.FloatArenaView;
import top.kzre.krro.util.arena.IntArenaView;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 项目核心运行时状态，一个全局资产库+多个场景
 */
public abstract class K3DRuntimeBase {

    private final SceneSchedulerHolderFactory animatorFactory;
    private final COWChannelHolder<ProjectAssetLib> assetLib;
    private final ConcurrentHashMap<Long, COWChannelHolder<ProjectScene>> scenes;
    private final ConcurrentHashMap<Long, SceneSchedulerHolder> sceneSchedulers;
    protected K3DRuntimeBase(SceneSchedulerHolderFactory schedulerFactory) {
        this.animatorFactory = schedulerFactory;
        assetLib = COWChannelHolder.create(ProjectAssetLib.create());
        scenes = new ConcurrentHashMap<>();
        sceneSchedulers = new ConcurrentHashMap<>();
    }

    public SceneScheduler getSceneScheduler(long sceneId) {
        if (!scenes.containsKey(sceneId)) {
            throw new IllegalArgumentException("Scene " + sceneId + " not found");
        }
        // 获取调度器，懒创建
        SceneSchedulerHolder holder = sceneSchedulers.computeIfAbsent(sceneId,
                animatorFactory::create);
        return holder.getSceneScheduler();
    }

    /**
     * 读取项目数据
     */
    public void readAssetLib(Consumer<ProjectAssetLib> consumer) throws Exception {
        assetLib.read(consumer);
    }

    /**
     * 写入项目数据
     */
    public void writeAssetLib(Consumer<ProjectAssetLib> consumer) throws Exception {
        assetLib.write(consumer);
    }

    public void createScene(long sceneId){
        scenes.computeIfAbsent(sceneId,
                aLong -> COWChannelHolder.create(
                        ProjectScene.create(sceneId, newFloatArenaTemplate())
                ));
    }

    public void destroyScene(long sceneId){
        // 先删场景，后续就不会再创建调度器了
        COWChannelHolder<ProjectScene> removed = scenes.remove(sceneId);
        SceneSchedulerHolder holder = sceneSchedulers.remove(sceneId);

        // 但关闭时候，调度器先关闭
        RuntimeException ex = ResourceUtils.closeDelayError(holder, removed);
        if (ex != null) {
            throw ex;
        }
    }

    public void readScene(long sceneId, Consumer<ProjectScene> consumer) throws Exception {
        COWChannelHolder<ProjectScene> holder = scenes.get(sceneId);
        if (holder == null) {
            throw new NullPointerException("scene does not exist");
        }
        holder.read(consumer);
    }

    public void writeScene(long sceneId, Consumer<ProjectScene> consumer) throws Exception {
        COWChannelHolder<ProjectScene> holder = scenes.get(sceneId);
        if (holder == null) {
            throw new NullPointerException("scene does not exist");
        }
        holder.write(consumer);
    }

    public void close(){
        // 关闭调度器
        RuntimeException ex0 = ResourceUtils.closeDelayError(sceneSchedulers.values());
        // 关闭场景
        Throwable ex1 = ResourceUtils.closeDelayError(scenes.values());
        // 关闭资产库
        Throwable ex2 = null;
        try {
            assetLib.close();
        } catch (Exception e) {
            ex2 = e;
        }
        // 关闭资产库
        Throwable finalEx = ResourceUtils.accumulateException(ex0, ex1, ex2);
        if (finalEx != null) {
            throw ResourceUtils.wrapError(finalEx);
        }
    }

    /**
     * 创建 float 全局分配池
     */
    public abstract ArenaTemplate<?, FloatArenaView> newFloatArenaTemplate();

    /**
     * 创建 Int 全局分配池
     */
    public abstract ArenaTemplate<?, IntArenaView> newIntArenaTemplate();
}
