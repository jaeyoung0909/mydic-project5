(ns mydic.commands
  (:require [mydic.dict-api.core :as dict-api]
            [re-frame.core :as rf]
            [clojure.core.async :as async :refer [go <!]]))

;; 가끔 먼저 보낸 요청이 나중에 오는 경우를 걸러줘야 함
(def last-word-comp (atom ""))

(defn get-word-completion
  [prefix]
  (reset! last-word-comp prefix)
  (rf/dispatch [:word-search/start-completion])
  (go (let [result (<! (dict-api/word-completion prefix))]
        (when (= @last-word-comp (:query result))
          (rf/dispatch [:word-search/on-completion (:list result)])))))

(defn get-word-summary
  "word-link -> word-summary"
  [{:keys [word definition id]}]
  (go (let [result (<! (dict-api/word-summary id))]
        (rf/dispatch [:word-search/on-receiving-definition result]))))

(defn search-word-and-select
  [query]
  (rf/dispatch [:word-search/start-search])
  (go (let [result            (<! (dict-api/word-detailed-search query))
            words             (:result result)
            {:keys [word id] :as word-link} (first words)]
        (when (seq words)
          (rf/dispatch [:word-search/select word :definition id])
          (get-word-summary word-link))
        (rf/dispatch [:word-search/on-search-result words]))))




  
