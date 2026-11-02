(ns top.kzre.krro.plugin.modeling.core.edit.core
  "视口导航分派。采用 Blender 规范的默认键位：

    滚轮                    → dolly（缩放）
    中键拖拽                → orbit（旋转）
    Shift + 中键拖拽        → pan（平移）
    Ctrl  + 中键拖拽        → dolly（拖拽缩放）
    左键                    → 不归视口（选择/工具，上层处理）
    右键                    → 不归视口（上下文菜单，上层处理）

    判断顺序：scroll 先于 button 分支——scroll 没有 :mouse-button，
    不能落入按钮分支。修饰键分支先于裸 MMB，否则 Ctrl+MMB 会被裸 MMB
    分支截走，dolly 触发不了。"
  (:require [top.kzre.krro.plugin.modeling.core.edit.dispatch :as dispatch]))


(defmethod dispatch/tool-event :viewport
  [_ {:keys [mouse-button modifiers type] :as _event-map}]
  (let [{:keys [shift ctrl alt]} modifiers]
    (cond
      ;; 滚轮：dolly，不区分按钮、不区分修饰键
      (= type :scroll)
      :viewport-dolly/wheel

      ;; 无按钮事件（move/over）或非导航按钮：不触发
      (not (#{:left :middle :right} mouse-button))
      nil

      ;; 修饰键分支必须先于裸 MMB 分支：Ctrl/Shift + MMB 是不同操作
      (and (= mouse-button :middle) shift)
      (case type
        :press   :viewport-pan/press
        :drag    :viewport-pan/drag
        :release :viewport-pan/release
        nil)

      (and (= mouse-button :middle) ctrl)
      (case type
        :press   :viewport-dolly/press
        :drag    :viewport-dolly/drag
        :release :viewport-dolly/release
        nil)

      ;; 裸 MMB：orbit
      (= mouse-button :middle)
      (case type
        :press   :viewport-orbit/press
        :drag    :viewport-orbit/drag
        :release :viewport-orbit/release
        nil)

      ;; 左键 / 右键：归工具或上下文菜单，不归视口导航
      :else nil)))