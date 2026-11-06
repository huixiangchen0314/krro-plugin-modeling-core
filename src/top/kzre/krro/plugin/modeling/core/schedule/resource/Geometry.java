package top.kzre.krro.plugin.modeling.core.schedule.resource;

import top.kzre.krro.d3.core.util.cow.*;

/**
 * 几何资源id
 */
public final class Geometry {
    private  Geometry() {
    }

    public static
    ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteFloats>>>>
     cornerPositions(){
        // 对象Id->顶点位置
        return ResourceKey.of("geometry.cornerPositions");
    }

    public static
    ResourceKey<ResourceTable<CopyOnWriteObject<ListResource<CopyOnWriteInts>>>>
    triangleIndices(){
        // 对象Id->顶点位置
        return ResourceKey.of("geometry.triangleIndices");
    }
}
