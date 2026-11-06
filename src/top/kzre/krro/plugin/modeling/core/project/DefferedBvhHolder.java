package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.d3.core.geometry.bvh.BVH;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 快建 lbvh
 *  <br>
 * 慢建 binned-bvh
 */
public final class DefferedBvhHolder implements BvhHolder {
    private final AtomicReference<BVH> bvh;

    private DefferedBvhHolder(AtomicReference<BVH> bvh) {
        this.bvh = bvh;
    }

    public static DefferedBvhHolder create(BVH bvh) {
        if (bvh == null) {
            throw new NullPointerException("bvh is null");
        }
        return new DefferedBvhHolder(new AtomicReference<>(bvh));
    }

    public static DefferedBvhHolder create() {
        return new DefferedBvhHolder(new AtomicReference<>());
    }

    @Override
    public BVH getBvh() {
        BVH result = bvh.get();
        if (result == null) {
            throw new NullPointerException("BVH is null");
        }
        return result;
    }

    public void setBvh(BVH bvh) {
        this.bvh.set(bvh);
    }
}
