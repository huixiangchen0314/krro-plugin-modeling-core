package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.d3.core.geometry.bvh.BVH;

/**
 * 默认的 BVH 持有者，立即持有
 */
public final class DefaultBvhHolder implements BvhHolder {
    private final BVH bvh;

    public DefaultBvhHolder(BVH bvh) {
        this.bvh = bvh;
    }

    @Override
    public BVH getBvh() {
        return bvh;
    }
}
