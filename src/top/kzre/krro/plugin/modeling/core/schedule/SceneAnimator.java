package top.kzre.krro.plugin.modeling.core.schedule;

import java.util.concurrent.locks.LockSupport;

public final class SceneAnimator implements Runnable, SceneSchedulerHolder {
    private final AbstractSceneScheduler scheduler;
    private final long frameNanos;
    private final Thread thread;
    private volatile boolean running = true;
    // TODO 脏标记，防止忙等

    public SceneAnimator(AbstractSceneScheduler scheduler,
                         long frameNanos) {
        this.scheduler = scheduler;
        this.frameNanos = frameNanos;
        this.thread = new Thread(this, "scene-animator-" + scheduler.getSceneId());
        this.thread.setDaemon(true);
        // 创建即渲染
        this.thread.start();
    }

    @Override
    public SceneScheduler getSceneScheduler() {
        return scheduler;
    }

    @Override
    public void run() {
        long next = System.nanoTime();
        while (running) {
            scheduler.poll();
            next += frameNanos;
            long sleep = next - System.nanoTime();
            if (sleep > 0) {
                LockSupport.parkNanos(sleep);
            } else {
                next = System.nanoTime();   // 落后不追帧
            }
        }
    }

    @Override
    public void close() {
        running = false;
        LockSupport.unpark(thread);
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.close();
    }
}
