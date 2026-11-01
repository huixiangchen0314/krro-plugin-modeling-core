(ns top.kzre.krro.plugin.modeling.core.project.scene
  "逐场景 reframe 字段定义"
  (:require
   [clojure.spec.alpha :as s]
   [taoensso.timbre :as log]
   [top.kzre.krro.core.rdb :refer [defschema]]
   [top.kzre.krro.core.reframe.core :as rf]
   [top.kzre.krro.core.util.ref-count :as ref-count]))

(s/def ::id integer?)

(s/def ::scene-data
  (s/keys
    :req-un [::id]))

(defschema :krro.modeling/scene
           :primary-key :id
           :spec ::scene-data
           )

(defn make-scene-data
  [scene-id]
  {:id scene-id})

;; 逐场景事件
(defonce ^:const app-id :krro-modeling)

;; 数据不直接进入 project 原子
(defonce ^:private record-data (atom {}))

(defn record [scene-id]
  (merge
    (get @record-data scene-id)
    {:scene-id   scene-id}))

(defn- update-record! [{:keys [scene-id]
                        :as record}]
  ;; 持久化数据记录进 project 原子
  (swap! record-data assoc scene-id record))

(defn- scene-getter [record-id]
  (record record-id))

(defn- scene-setter [record-id new-record]
  ;; 保证 record-id 一致
  (update-record! (assoc new-record :scene-id record-id)))

(defn reg-store [scene-id]
  (ref-count/acquire
    [::scene scene-id]
    (fn []
      (let [stop-fn (rf/reg-store app-id scene-id scene-getter scene-setter)]
        (fn []
          (stop-fn)
          (swap! record-data dissoc scene-id)
          (log/info "unregistered store for scene-id:" scene-id))))))
