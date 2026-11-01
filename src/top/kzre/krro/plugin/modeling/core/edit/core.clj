(ns top.kzre.krro.plugin.modeling.core.edit.core
  "全局编辑的总部"
  (:require [top.kzre.krro.plugin.modeling.core.edit.dispatch :as dispatch]))


(defmethod dispatch/tool-event :viewport
  [_ event-map]
  (case (:type event-map)
    :press :viewport-pan/press
    :drag :viewport-pan/drag
    :scroll :viewport-pan/scroll
    :release :viewport-pan/release
    nil))