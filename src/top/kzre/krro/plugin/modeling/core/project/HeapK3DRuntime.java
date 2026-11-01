package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.plugin.modeling.core.schedule.SceneSchedulerHolderFactory;
import top.kzre.krro.util.arena.*;

public final class HeapK3DRuntime extends K3DRuntimeBase{
    private final FloatArrayArena floatArena;
    private final IntArrayArena intArena;

    public HeapK3DRuntime(SceneSchedulerHolderFactory schedulerFactory) {
        super(schedulerFactory);

        floatArena = Arenas.newEmptyFloatArray();
        intArena = Arenas.newEmptyIntArray();
    }

    @Override
    public ArenaTemplate<?, FloatArenaView> newFloatArenaTemplate() {
        return AutoGrowFloatArrayArenaTemplate.fixedSize(floatArena, 1024 * 1024);
    }

    @Override
    public ArenaTemplate<?, IntArenaView> newIntArenaTemplate() {
        return AutoGrowIntArrayArenaTemplate.fixedSize(intArena, 1024 * 1024);
    }
}
