package top.kzre.krro.plugin.modeling.core.object.intersection;

import top.kzre.krro.d3.core.geometry.Ray;
import top.kzre.krro.d3.core.geometry.RayHit;
import top.kzre.krro.d3.core.scene.SceneObject;
import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;

public interface RayObjectIntersection {

    /**
     * 射线，对象相交策略。返回射线结果。
     * Ray 已经是本地射线了，方向是不归一化的
     * t 在仿射变换下不变，因此结果可直接跨对象比较
     */
    RayHit intersect(Ray localRay, SceneObject obj, ProjectAssetLib assetLib);
}
