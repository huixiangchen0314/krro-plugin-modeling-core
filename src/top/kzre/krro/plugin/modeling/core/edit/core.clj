(ns top.kzre.krro.plugin.modeling.core.edit.core
  "全局编辑的总部"
  (:require [top.kzre.krro.plugin.modeling.core.edit.dispatch :as dispatch]))


(defmethod dispatch/tool-event :viewport
  [_ {:keys [ mouse-button type]}]
  (case mouse-button
    :middle
    (case type
      :press :viewport-pan/press
      :drag :viewport-pan/drag
      :scroll :viewport-pan/scroll
      :release :viewport-pan/release
      nil)
    :left
    (case type
      :press :viewport-orbit/press
      :drag :viewport-orbit/drag
      :scroll :viewport-orbit/scroll
      :release :viewport-orbit/release
      nil)
    nil)
  )