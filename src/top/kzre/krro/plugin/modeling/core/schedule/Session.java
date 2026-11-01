package top.kzre.krro.plugin.modeling.core.schedule;


import top.kzre.krro.core.util.IChannel;
import top.kzre.krro.util.tile.TiledCanvas;

/**
 * 渲染会话, 绑定到一个场景的视角，不保证线程安全，用户处单写
 */
public interface Session {
    String getRenderId();
    /**
     * 设置视口大小
     */
    void setViewSize(int width, int height);

    void setViewProjection(float[] viewProjMat4);

    /**
     * go CSP  风格的通道接口
     */
    IChannel<TiledCanvas> viewChannel();

    void setOnDirty(Runnable cb);

    /**
     * 关闭会话就是关闭通道。场景调度器自己会清理
     */
    default void close(){
        IChannel<TiledCanvas> chan = viewChannel();
        // channel 的关闭, 可重入
        chan.close();
    }

    default boolean isClosed(){
        IChannel<TiledCanvas> chan = viewChannel();
        return chan.isClosed();
    }

    boolean isValid();
}
