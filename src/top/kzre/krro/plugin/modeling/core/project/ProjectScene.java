package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.d3.core.scene.HierarchyEditor;
import top.kzre.krro.d3.core.scene.SceneObject;
import top.kzre.krro.d3.core.util.LongObjectMap;
import top.kzre.krro.d3.core.util.cow.CopyOnWrite;
import top.kzre.krro.d3.core.util.cow.CopyOnWriteFloats;
import top.kzre.krro.util.arena.ArenaTemplate;
import top.kzre.krro.util.arena.FloatArenaView;
import top.kzre.krro.util.math.KMath;

import java.util.ArrayList;

// TODO 代理内部结构
public final class ProjectScene implements CopyOnWrite<ProjectScene> {

    // 场景数据
    private final HierarchyEditor hierarchyEditor;
    private final ArrayList<SceneObject> removeScratch = new ArrayList<>();
    // 其他配置项，如渲染设置等等

    private ProjectScene(HierarchyEditor hierarchyEditor) {
        this.hierarchyEditor = hierarchyEditor;

    }

    /**
     * 创建项目场景
     */
    public static ProjectScene create(long sceneId,
                                      ArenaTemplate<?, FloatArenaView> floatArena) {
        return new ProjectScene(HierarchyEditor.create(sceneId, floatArena));
    }

    /**
     * Objects 只读
     */
    public LongObjectMap<SceneObject> objects(){
        return hierarchyEditor.objects();
    }

    public LongObjectMap<CopyOnWriteFloats> globalTransforms(){
        return hierarchyEditor.globalTransforms();
    }

    /**
     * 添加物体，放到场景根位置,
     * 直接代理子结构，不要换名字
     */
    public void addRoot(SceneObject object){
        hierarchyEditor.addRoot(object);
    }

    /**
     * 添加物体，指定父物体
     */
    public void addChild(long parentId, SceneObject object){
        hierarchyEditor.addChild(parentId, object, KMath.mat4Identity());
    }

    /**
     * 移除场景物体，关闭 Autocloseable
     */
    public void removeObject(long objId){
        removeScratch.clear();
        hierarchyEditor.removeNode(objId, removeScratch);
        Exception ex = null;
        for (SceneObject sceneObject : removeScratch) {
            if (sceneObject instanceof AutoCloseable){
                try {
                    ((AutoCloseable)sceneObject).close();
                } catch (Exception e) {
                    if (ex == null) {
                        ex = e;
                    }else {
                        ex.addSuppressed(e);
                    }
                }
            }
        }
        removeScratch.clear();
        if (ex != null) {
            throw new RuntimeException(ex);
        }
    }

    public boolean getInvTransform(long objId, float[] mat4){
       return hierarchyEditor.getInvGlobalTransform(objId, mat4);
    }

    @Override
    public ProjectScene shared() {
        return new ProjectScene(hierarchyEditor.shared());
    }

    @Override
    public void close() {
        hierarchyEditor.close();
    }
}
