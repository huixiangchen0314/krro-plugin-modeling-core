package top.kzre.krro.plugin.modeling.core.project.accel;

import top.kzre.krro.d3.core.geometry.SpatialOverlay;

public class SpatialOverlayUtils {

    /**
     * 将 SpatialOverlay 查询结果转为 longs
     */
    public static long[] indexToId(SpatialOverlay o, long[] idIndex){
        if (o.isEmpty()){
            return new long[0];
        }
        // 去重
         o = SpatialOverlay.distinct(o);
        int n = o.size();
        long[] result = new long[n];
        for (int i = 0; i < n; i++) {
            int idx = o.get(i);
            result[i] = idIndex[idx];
        }
        return result;
    }
}
