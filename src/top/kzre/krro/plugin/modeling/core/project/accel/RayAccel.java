package top.kzre.krro.plugin.modeling.core.project.accel;

import top.kzre.krro.d3.core.geometry.Ray;

/**
 * 射线穿过查询
 */
public interface RayAccel {
    /**
     * 射线查询->候选的 SceneObject#id/网格的faceOffset索引
     */
    long[] query(Ray ray);
}
