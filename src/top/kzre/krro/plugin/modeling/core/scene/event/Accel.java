package top.kzre.krro.plugin.modeling.core.scene.event;

import top.kzre.krro.plugin.modeling.core.project.K3DRuntime;
import top.kzre.krro.plugin.modeling.core.project.accel.RayAccel;
import top.kzre.krro.plugin.modeling.core.project.accel.SceneAccelFactory;

import java.util.concurrent.atomic.AtomicReference;

public final class Accel {
    private Accel(){}

    public static RayAccel buildSceneAccel(SceneAccelFactory factory, long sceneId) {
        AtomicReference<RayAccel> ref = new AtomicReference<>();
        try {
            K3DRuntime.readAssetLib(assetLib->{
                try {
                    K3DRuntime.readScene(sceneId, scene -> {
                        RayAccel accel = factory.create(assetLib, scene);
                        ref.set(accel);
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ref.get();
    }
}
