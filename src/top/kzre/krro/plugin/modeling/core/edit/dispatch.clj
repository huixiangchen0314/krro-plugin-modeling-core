(ns top.kzre.krro.plugin.modeling.core.edit.dispatch
  "事件分发"
  (:require
   [top.kzre.krro.core.reframe.core :as rf]
   [top.kzre.krro.plugin.modeling.core.project.scene :as scene]
   [top.kzre.krro.plugin.painting.core.edit.tool :as tool :refer [cleanup-tool-interceptor]]))

(defmulti tool-event
          (fn [current-tool event-map]
            (or current-tool :viewport)
            ;(if (and (= :pointer (:device-type event-map))
            ;         (= :middle (:mouse-button event-map)))
            ;  ;; 鼠标中键是视口工具
            ;  (let [ret :viewport]
            ;    ret)
            ;  current-tool)
            ))

(defmethod tool-event :default [_ _] nil)

;; 分发工具事件
(rf/reg-event-fx
  scene/app-id :tool/dispatch-event
  (tool/dispatch-fn tool-event))

(rf/reg-event-fx
  scene/app-id :tool/select-tool
  [(cleanup-tool-interceptor)]
  (fn [cofx [_ _ tool-id]]
    {:record
     (-> (:record cofx)
         (assoc-in [ :tool-data]
                   (case tool-id
                     ;; 默认状态
                     :anchor-translate nil
                     nil))
         (assoc-in [ :current-tool] tool-id))}))
