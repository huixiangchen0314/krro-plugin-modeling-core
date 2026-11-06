package top.kzre.krro.plugin.modeling.core.object;

import top.kzre.krro.d3.core.geometry.Ray;
import top.kzre.krro.d3.core.geometry.RayHit;
import top.kzre.krro.d3.core.geometry.algo.RayFaceIntersection;
import top.kzre.krro.d3.core.scene.AssetMeshRef;
import top.kzre.krro.d3.core.scene.SceneObject;
import top.kzre.krro.d3.core.util.LongLongMap;
import top.kzre.krro.d3.core.util.LongObjectMap;
import top.kzre.krro.plugin.modeling.core.object.intersection.RayObjectIntersectionDispatcher;
import top.kzre.krro.plugin.modeling.core.project.K3DRuntime;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 对象拾取——在候选对象中选射线最近命中的那个。
 */
public final class Select {

    private Select() {
    }

    /**
     * 进行选择筛选，返回被选的物体 id。返回 {@code null} 表示无命中。
     *
     * <p><b>流程</b>：
     * <ol>
     *   <li>对每个候选对象：{@code scene.getInvTransform} 取世界→本地矩阵，
     *       {@link Ray#affine} 变换射线到本地空间</li>
     *   <li>{@code accel.query(localRay)} 拿到候选面（BVH 粗筛）</li>
     *   <li>排序候选面——面索引升序 ⇒ {@code faceOffsets[f]} 单调，便于顺序读取</li>
     *   <li>逐面调 {@link RayFaceIntersection#compute} 做精确求交（N 边形）</li>
     *   <li>跟踪最小 t，跨对象比较取最近者</li>
     * </ol>
     *
     * <p><b>t 精度</b>：{@code t} 用 {@code double} 跟踪，与
     * {@link RayFaceIntersection.Result#getT()} 的 double 返回值一致。
     *
     * <p><b>退化处理</b>：
     * <ul>
     *   <li>对象不在场景 / 非 {@link AssetMeshRef} / 无 accel / 无逆矩阵——跳过</li>
     *   <li>候选面为空——跳过</li>
     *   <li>所有面都 miss——不参与最近者竞争</li>
     * </ul>
     */
    public static Long select(long sceneId,
                              Ray ray,
                              long[] candidates) {
        if (ray == null) throw new NullPointerException("ray");
        if (candidates == null) throw new NullPointerException("candidates");

        float[] inv = new float[16];
        AtomicReference<Long> result = new AtomicReference<>();
        // intersession 可能有 内部状态，每个并行创建一个对象
        RayObjectIntersectionDispatcher intersection = new RayObjectIntersectionDispatcher();
        try {
            K3DRuntime.readAssetLib(assetLib -> {
                try {
                    K3DRuntime.readScene(sceneId, scene -> {
                        LongObjectMap<SceneObject> objects = scene.objects();

                        long closestObj = LongLongMap.UNIT_VALUE;
                        double closestT = Double.MAX_VALUE;

                        // TODO 对候选对象并行，这里读是按表分割的，不会重合，因此并行安全
                        for (long objId : candidates) {
                            SceneObject obj = objects.get(objId);
                            if (!scene.getInvTransform(objId, inv)) continue;

                            Ray localRay = Ray.affine(ray, inv);
                            RayHit hit = intersection.intersect(localRay, obj, assetLib);

                            // Ray 不会对内部方向向量进行归一化，因此 t 可直接比较
                            if (hit.hit && hit.t < closestT) {
                                closestT = hit.t ;
                                closestObj = objId;
                            }
                        }

                        if (!LongLongMap.isUnitValue(closestObj)) {
                            result.set(closestObj);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result.get();
    }
}