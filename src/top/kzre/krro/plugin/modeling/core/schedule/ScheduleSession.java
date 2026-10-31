package top.kzre.krro.plugin.modeling.core.schedule;

import top.kzre.krro.core.util.BufferedChannel;
import top.kzre.krro.core.util.IChannel;
import top.kzre.krro.util.tile.TiledCanvas;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基础抽象实现， 子类持有具体资源
 */
public abstract class ScheduleSession implements Session {
    private int width;
    private int height;

    private final IChannel<TiledCanvas> viewChannel;
    // 用户自己保证只在 UI 线程改, 我们可见就好
    private volatile Runnable onDirty;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    protected ScheduleSession() {
        // 容量1，用户永远拿到最新的画面，注意如果用户获取迟了，可能尺寸和请求时候不是一样的，自己做尺寸适配
        viewChannel = new BufferedChannel<>(1, TiledCanvas::close);
    }

    @Override
    public void setViewSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public IChannel<TiledCanvas> viewChannel() {
        return viewChannel;
    }

    @Override
    public void setOnDirty(Runnable cb) {
        this.onDirty = cb;
    }

    public boolean queryClosed(){
        // 快速路径
        if (closed.get()){
            return true;
        }

        // 查询时候触发失效检查
        if (viewChannel.isClosed()){
            if (closed.compareAndSet(false, true)) {
                onClose();
            }
            return true;
        }

        return false;
    }

    /**
     * 关闭会话，然后立即查询失效。调度器关闭时候使用
     */
    public void release(){
        close();
        queryClosed();
    }

    public void sendView(TiledCanvas canvas) {
        viewChannel.send(canvas);
        if(this.onDirty != null){
            this.onDirty.run();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    protected abstract void onClose();

}
