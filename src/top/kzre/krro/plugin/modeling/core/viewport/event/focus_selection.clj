(ns top.kzre.krro.plugin.modeling.core.viewport.event.focus-selection
  "聚焦选中对象（frame selected）的事务。

  语义：把选中集的包围盒适配到视口——
    1. 计算选中集的世界空间包围盒（从 ProjectScene 的共享快照读）。
    2. target ← 包围盒中心。
    3. distance / ortho-scale ← 使包围盒完整落入视野的适配值。
    相机朝向（azimuth/elevation）不变，只移动注视点和距离。")
