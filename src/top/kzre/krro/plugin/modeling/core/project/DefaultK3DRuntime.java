package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.plugin.modeling.core.project.accel.MeshAccelFactory;
import top.kzre.krro.plugin.modeling.core.schedule.SceneSchedulerHolderFactory;
import top.kzre.krro.util.arena.*;

public final class DefaultK3DRuntime extends K3DRuntimeBase{

    private DefaultK3DRuntime(
            SceneSchedulerHolderFactory schedulerFactory,
            MeshAccelFactory makeFaceBvh,
            ArenaTemplate<?, FloatArenaView> floatArena) {
        super(schedulerFactory, makeFaceBvh, floatArena);
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        private SceneSchedulerHolderFactory schedulerFactory;
        private MeshAccelFactory makeFaceBvh;
        private ArenaTemplate<?, FloatArenaView> floatArenaTemplate;

        public Builder schedulerFactory(SceneSchedulerHolderFactory schedulerFactory){
            this.schedulerFactory = schedulerFactory;
            return this;
        }

        public Builder meshAccelFactory(MeshAccelFactory meshAccelFactory){
            this.makeFaceBvh = meshAccelFactory;
            return this;
        }

        public Builder floatArenaTemplate(ArenaTemplate<?, FloatArenaView> tpl){
            this.floatArenaTemplate = tpl;
            return this;
        }

        public DefaultK3DRuntime build(){
            if(schedulerFactory == null){
                throw new NullPointerException("schedulerFactory");
            }
            if (makeFaceBvh == null){
                throw new NullPointerException("makeFaceBvh");
            }
            if(floatArenaTemplate == null){
                throw new NullPointerException("floatArena");
            }

            return new DefaultK3DRuntime(schedulerFactory, makeFaceBvh, floatArenaTemplate);
        }
    }

}
