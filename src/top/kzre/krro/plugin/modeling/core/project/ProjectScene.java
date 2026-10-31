package top.kzre.krro.plugin.modeling.core.project;

import top.kzre.krro.d3.core.scene.Scene;
import top.kzre.krro.d3.core.scene.SceneEditor;
import top.kzre.krro.d3.core.util.cow.CopyOnWrite;
import top.kzre.krro.util.arena.ArenaTemplate;
import top.kzre.krro.util.arena.FloatArenaView;

public final class ProjectScene implements CopyOnWrite<ProjectScene> {

    // 场景数据
    private final SceneEditor sceneEditor;

    // 其他配置项，如渲染设置等等

    private ProjectScene(SceneEditor sceneEditor) {
        this.sceneEditor = sceneEditor;
    }

    /**
     * 创建项目场景
     */
    public static ProjectScene create(long sceneId,
                                      ArenaTemplate<?, FloatArenaView> floatTemplate) {
        Scene scene = Scene.create(sceneId);
        SceneEditor editor = new SceneEditor(scene, floatTemplate);
        return new ProjectScene(editor);
    }


    public SceneEditor getSceneEditor() {
        return sceneEditor;
    }

    @Override
    public ProjectScene shared() {
        return new ProjectScene(sceneEditor);
    }

    @Override
    public void close() {
        sceneEditor.close();
    }
}
