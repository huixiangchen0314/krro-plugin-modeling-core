package top.kzre.krro.plugin.modeling.core.project.accel;

import top.kzre.krro.d3.core.geometry.ExtendedMesh;

/**
 * 网格查询加速工厂
 */
public interface MeshAccelFactory
{
    RayAccel create(ExtendedMesh mesh);
}
