package top.kzre.krro.plugin.modeling.core.project.accel;

import top.kzre.krro.d3.core.asset.MeshAsset;
import top.kzre.krro.d3.core.geometry.AABB;
import top.kzre.krro.d3.core.geometry.IAABB;
import top.kzre.krro.d3.core.geometry.bvh.BVH;
import top.kzre.krro.d3.core.scene.AssetMeshRef;
import top.kzre.krro.d3.core.scene.SceneObject;
import top.kzre.krro.d3.core.util.LongObjectMap;
import top.kzre.krro.d3.core.util.cow.CopyOnWriteFloats;
import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;
import top.kzre.krro.plugin.modeling.core.project.ProjectScene;
import top.kzre.krro.util.arena.FloatArenaView;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public final class BVHSceneAccelFactory implements SceneAccelFactory {
    private final BiFunction<float[], Long, BVH> makeBvh;
    private final BiFunction<BVH, long[], RayAccel> makeAccel;
    private final float[] mat4Scratch = new float[16];

    /**
     *
     * @param makeBvh AABBArray->BVH
     * @param makeAccel BVH->id-index->SceneAccel
     */
    public BVHSceneAccelFactory(BiFunction<float[], Long, BVH> makeBvh,
                                BiFunction<BVH, long[], RayAccel> makeAccel) {
        this.makeBvh = makeBvh;
        this.makeAccel = makeAccel;
    }


    @Override
    public RayAccel create(ProjectAssetLib assetLib, ProjectScene scene) {

        // 1 网格id->网格aabb
        LongObjectMap<IAABB> assetAABBMap = new LongObjectMap<>();
        List<MeshAsset> meshAssets = assetLib.meshes().items().getSnapshot().getList();
        for (MeshAsset meshAsset : meshAssets) {
            if (meshAsset == null){
                continue;
            }
            IAABB bound = meshAsset.mesh().bounds();
            assetAABBMap.put(meshAsset.assetId(), bound);
        }

        AABB tmpAABB = new AABB();
        LongObjectMap<IAABB> aabbMap = new LongObjectMap<>();

        // 2. 物体->aabb
        LongObjectMap<SceneObject> objects = scene.objects();
        LongObjectMap<CopyOnWriteFloats> transMap = scene.globalTransforms();
        AtomicInteger count = new AtomicInteger();
        objects.forEach((s, sceneObject) -> {
            if ((sceneObject instanceof AssetMeshRef)) {
                AssetMeshRef meshRef = (AssetMeshRef) sceneObject;
                long assetId = meshRef.assetId();
                IAABB iaabb = assetAABBMap.get(assetId);

                CopyOnWriteFloats t = transMap.get(s);
                if (t != null) {
                    try (FloatArenaView.ArraySession session = t.array()) {
                        float[] floatsSnapshot = session.getFloats();
                        int of = session.offset();
                        System.arraycopy(floatsSnapshot, of, mat4Scratch, 0, 16);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                    AABB.affine(iaabb, mat4Scratch, tmpAABB);
                    aabbMap.put(assetId, tmpAABB.toImmutable());
                }else {
                    aabbMap.put(assetId, iaabb);
                }
                count.getAndIncrement();
            }
        });

        // aabbs
        int n = count.get();

        float[] aabbs = new float[n * 6];
        long[] idIndex = new long[n];
        AtomicInteger index = new AtomicInteger();
        aabbMap.forEach((objId, aabb) -> {
            int idx = index.getAndIncrement();
            idIndex[idx] = objId;
            aabbs[idx * 6] = aabb.getMinX();
            aabbs[idx * 6 + 1] = aabb.getMinY();
            aabbs[idx * 6 + 2] = aabb.getMinZ();
            aabbs[idx * 6 + 3] = aabb.getMaxX();
            aabbs[idx * 6 + 4] = aabb.getMaxY();
            aabbs[idx * 6 + 5] = aabb.getMaxZ();
        });
        BVH bvh = makeBvh.apply(aabbs, (long) n);
        return makeAccel.apply(bvh, idIndex);
    }
}
