(ns top.kzre.krro.plugin.modeling.core.schedule.protocol)

;; 先做抽象
;; 1. 渲染器不持有资源，调度层持有，数据按语义拆成属性行
;; 2.

(defprotocol IRenderScheduler
  (render! [_ ctx]
    "执行一次渲染,
  返回 Promise<IPersistentMap>.

  {:canvas canvas ;; 最终画布，该画布所有权归用户. 总是全量渲染
  "))