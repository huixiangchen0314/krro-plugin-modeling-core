package top.kzre.krro.plugin.modeling.core.schedule;

import top.kzre.krro.plugin.modeling.core.object.IActiveObjectHolderFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 抽象场景调度器，规定了会话和渲染器的架构。
 */
public abstract class AbstractSceneScheduler implements SceneScheduler {
    protected final Map<String, SceneRenderer> renderers;
    protected final CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();

    protected final long sceneId;

    public AbstractSceneScheduler(Map<String, SceneRenderer> renderers,
                                  long sceneId) {
        this.sceneId = sceneId;

        if (renderers == null) throw new NullPointerException("renderers cannot be null");
        if (renderers.isEmpty()) throw new IllegalArgumentException("renderers cannot be empty");
        // 渲染器，静态配置，绝对不可变
        this.renderers = Collections.unmodifiableMap(new HashMap<>(renderers));
    }

    @Override
    public Session openSession(String rendererId) {
        SceneRenderer sceneRenderer = renderers.get(rendererId);
        if (sceneRenderer == null) {
            throw new IllegalStateException("Renderer " + rendererId + " not found");
        }
        Session session = sceneRenderer.createSession();
        sessions.add(session);
        return session;
    }

    @Override
    public long getSceneId() {
        return sceneId;
    }

    /**
     * 拉取单帧渲染.
     * 1. 场景渲染器声明资源。
     * 2. 调度器准备。 组织一层计算图执行一次计算，获取场景层计算结果。
     * 3. 渲染器渲染，推送结果。
     */
    public abstract void poll();

    public void close(){
        // 关闭每个会话
        for (Session session : sessions) {
            session.close();
        }
        // 渲染器本身没有关闭接口，那些是进程资源，一旦加载，到进程退出时候释放
    }
}
