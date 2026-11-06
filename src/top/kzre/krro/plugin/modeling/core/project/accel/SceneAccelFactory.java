package top.kzre.krro.plugin.modeling.core.project.accel;

import top.kzre.krro.plugin.modeling.core.project.ProjectAssetLib;
import top.kzre.krro.plugin.modeling.core.project.ProjectScene;

/**
 * 对象级别加速结构
 */
public interface SceneAccelFactory {
    RayAccel create(ProjectAssetLib assetLib,
                    ProjectScene scene);
}
