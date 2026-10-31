(ns top.kzre.krro.plugin.modeling.core.project.scene
  (:require
   [top.kzre.krro.plugin.modeling.core.project.asset :as asset])
  (:import
   (top.kzre.krro.plugin.modeling.core.project ProjectScene)))

(defonce ^:private scene*
         (ProjectScene/create 1 (asset/float-arena-template)))

(defn scene
  "返回默认场景"
  [] scene*)