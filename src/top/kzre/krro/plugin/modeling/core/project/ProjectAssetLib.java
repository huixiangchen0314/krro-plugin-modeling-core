package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.d3.core.asset.AssetLib;
import top.kzre.krro.d3.core.asset.MeshAsset;
import top.kzre.krro.d3.core.util.LongObjectMap;
import top.kzre.krro.d3.core.util.cow.*;
import top.kzre.krro.plugin.modeling.core.project.accel.MeshAccelFactory;
import top.kzre.krro.plugin.modeling.core.project.accel.RayAccel;

public final class ProjectAssetLib implements CopyOnWrite<ProjectAssetLib> {
    private final AssetLib d3Lib;
    /**
     * 物体面级别的 BVH
     * TODO 的加速支持
     */
    private final CopyOnWriteObject<LongObjectMapResource<RayAccel>> meshFaceBvhMap;

    private final MeshAccelFactory meshAccelFactory;
    public ProjectAssetLib(AssetLib d3Lib,
                           CopyOnWriteObject<LongObjectMapResource<RayAccel>> meshFaceBvhMap,
                           MeshAccelFactory meshAccelFactory) {
        this.d3Lib = d3Lib;
        this.meshFaceBvhMap = meshFaceBvhMap;
        this.meshAccelFactory = meshAccelFactory;
    }

    // 资产代理，BVH 维护
    public static ProjectAssetLib create(MeshAccelFactory meshAccelFactory){
        return new ProjectAssetLib(AssetLib.create(),
                new DefaultCopyOnWriteObject<>(new LongObjectMapResource<>(new LongObjectMap<>())), meshAccelFactory);
    }

    public ResourceTable<MeshAsset> meshes() {
        return d3Lib.meshes();
    }

    public MeshAsset getMeshAsset(long var1) {
        return d3Lib.getMeshAsset(var1);
    }

    /**
     * 获取网格面级别的 BVH. 形态键另外查询,
     * 没法放在这里。
     * <br>
     * 形态键是固化状态。positions是当前状态。
     * 当前状态改变不会对形态键产生任何印象。
     * 形态键的操作是重新指定，不存在局部修改的操作方式
     */
    public RayAccel meshAccel(long assetId){
        MeshAsset meshAsset = d3Lib.getMeshAsset(assetId);
        if(meshAsset == null){
            throw new IllegalArgumentException("Asset ID " + assetId + " doesn't exist");
        }
        RayAccel rayAccel = meshFaceBvhMap.getSnapshot().getMap().get(assetId);
        if(rayAccel != null){
            return rayAccel;
        }
         rayAccel = meshAccelFactory.create(meshAsset.mesh());

        meshFaceBvhMap.getForWrite().getMap().put(assetId, rayAccel);
        return rayAccel;
    }

    /**
     * 丢弃网格面Bvh 缓存
     */
    private void discardMeshFaceBvh(long assetId){
        meshFaceBvhMap.getForWrite().getMap().remove(assetId);
    }

    @Override
    public ProjectAssetLib shared() {
        return new ProjectAssetLib(d3Lib.shared(), meshFaceBvhMap.shared(), meshAccelFactory);
    }

    @Override
    public void close()  {
        d3Lib.close();
    }
}
