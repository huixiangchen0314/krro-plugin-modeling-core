package top.kzre.krro.plugin.modeling.core.object.intersection;

import top.kzre.krro.d3.core.asset.MeshAsset;
import top.kzre.krro.d3.core.geometry.ExtendedMesh;
import top.kzre.krro.d3.core.geometry.Ray;
import top.kzre.krro.d3.core.geometry.RayHit;
import top.kzre.krro.d3.core.geometry.algo.RayFaceIntersection;
import top.kzre.krro.d3.core.geometry.algo.RayFaceIntersectionImpl;
import top.kzre.krro.d3.core.scene.AssetMeshRef;
import top.kzre.krro.d3.core.scene.SceneObject;
import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;
import top.kzre.krro.plugin.modeling.core.project.accel.RayAccel;
import top.kzre.krro.util.arena.IntArenaView;

public final class RayMeshIntersection implements RayObjectIntersection {
    private final RayFaceIntersection intersector = new RayFaceIntersectionImpl();
    private final RayFaceIntersection.Result result = RayFaceIntersection.Result.create();

    @Override
    public RayHit intersect(Ray localRay, SceneObject obj, ProjectAssetLib assetLib) {
        AssetMeshRef assetMeshRef = (AssetMeshRef) obj;
        MeshAsset meshAsset = assetLib.getMeshAsset(assetMeshRef.assetId());
        if (meshAsset == null){
            throw new RuntimeException("Asset ID " + assetMeshRef.assetId() + " not found");
        }

        RayAccel accel = assetLib.meshAccel(assetMeshRef.assetId());
        if (accel == null) {
            throw new RuntimeException("Asset ID " + assetMeshRef.assetId() + " not found");
        }

        long[] faces = accel.query(localRay);
        if (faces.length == 0) return RayHit.MISS;

        ExtendedMesh mesh = meshAsset.mesh();
        if (mesh == null) return RayHit.MISS;

        double objMinT = Double.MAX_VALUE;

        // faceOffsets 是 int[]——只开这一个 session 即可。
        // 不需要在这里开 positions / cornerVerts 的 session：
        // RayFaceIntersectionImpl.compute 内部会自己开。
        // session 可重入，可只读并行，不过对象内的并行效益不大，外层去并行
        try (IntArenaView.ArraySession foSess = mesh.faceOffsets().array()) {
            int[] fo = foSess.getInts();
            int foOff = foSess.offset();

            // 对象
            for (long face : faces) {
                int f = (int) face;
                int faceStart = fo[foOff + f];
                int nPoly = fo[foOff + f + 1] - faceStart;

                intersector.compute(localRay,
                        mesh.positions(),
                        mesh.cornerVerts(),
                        faceStart, nPoly,
                        result);

                if (result.isHit()) {
                    double t = result.getT();
                    if (t < objMinT) objMinT = t;
                }
            }
        }
        return RayHit.of((float) objMinT);
    }
}
