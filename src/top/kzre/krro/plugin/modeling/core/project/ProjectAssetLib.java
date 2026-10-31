package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.d3.core.asset.AssetLib;
import top.kzre.krro.d3.core.asset.MeshAsset;
import top.kzre.krro.d3.core.util.cow.CopyOnWrite;
import top.kzre.krro.d3.core.util.cow.ResourceTable;

public final class ProjectAssetLib implements CopyOnWrite<ProjectAssetLib> {
    private final AssetLib d3Lib;

    public ProjectAssetLib(AssetLib d3Lib) {
        this.d3Lib = d3Lib;
    }

    public static ProjectAssetLib create(){
        return new ProjectAssetLib(AssetLib.create());
    }

    public ResourceTable<MeshAsset> meshes() {
        return d3Lib.meshes();
    }

    public MeshAsset getMeshAsset(long var1) {
        return d3Lib.getMeshAsset(var1);
    }

    @Override
    public ProjectAssetLib shared() {
        return new ProjectAssetLib(d3Lib.shared());
    }

    @Override
    public void close()  {
        d3Lib.close();
    }
}
