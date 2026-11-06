package top.kzre.krro.plugin.modeling.core.object.intersection;

import top.kzre.krro.d3.core.geometry.Ray;
import top.kzre.krro.d3.core.geometry.RayHit;
import top.kzre.krro.d3.core.scene.AssetMeshRef;
import top.kzre.krro.d3.core.scene.SceneObject;
import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;

public final class RayObjectIntersectionDispatcher implements RayObjectIntersection {
    private final RayMeshIntersection rayMeshIntersection = new RayMeshIntersection();
    @Override
    public RayHit intersect(Ray localRay, SceneObject obj, ProjectAssetLib assetLib) {
        if (obj instanceof AssetMeshRef){
            return rayMeshIntersection.intersect(localRay, obj, assetLib);
        }

        throw new UnsupportedOperationException("Unknown object type" + obj.getClass().getName());
    }
}
