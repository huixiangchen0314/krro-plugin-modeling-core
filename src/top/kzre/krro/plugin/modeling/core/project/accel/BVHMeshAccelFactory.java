package top.kzre.krro.plugin.modeling.core.project.accel;

import top.kzre.krro.d3.core.geometry.ExtendedMesh;
import top.kzre.krro.d3.core.geometry.bvh.BVH;
import top.kzre.krro.d3.core.util.cow.CopyOnWriteFloats;
import top.kzre.krro.plugin.modeling.core.util.TriFunction;
import top.kzre.krro.util.arena.FloatArenaView;

import java.util.function.Function;

public final class BVHMeshAccelFactory implements MeshAccelFactory{
    private final TriFunction<float[], Long,Long,  BVH> makeBvh;
    private final Function<BVH, RayAccel> makeAccel;

    public BVHMeshAccelFactory(TriFunction<float[], Long,Long,  BVH> makeBvh,
                               Function<BVH, RayAccel> makeAccel) {
        this.makeBvh = makeBvh;
        this.makeAccel = makeAccel;
    }

    @Override
    public RayAccel create(ExtendedMesh mesh) {
        CopyOnWriteFloats aabbs = mesh.faceAabbs();
        long faceCount = mesh.faceCount();
        BVH bvh ;
        try (FloatArenaView.ArraySession session = aabbs.array()) {
            float[] floats = session.getFloats();
            long offset = session.offset();
            bvh = makeBvh.apply(floats, faceCount, offset);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return makeAccel.apply(bvh);
    }
}
