package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.d3.core.geometry.bvh.BVH;

/**
 * Bvh Holder 模式，用于封装Bvh 的快慢建
 */
public interface BvhHolder {
    BVH getBvh();
}
