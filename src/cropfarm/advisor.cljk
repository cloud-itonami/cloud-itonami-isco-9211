(ns cropfarm.advisor
  "FieldLabourAdvisor — the advisor named in this repository's
  README, proposing a field operation (approve a work session,
  approve heavy-equipment proximity, approve water-source treatment)
  from a work order, field plan and safety brief. Swappable mock/llm;
  the advisor ONLY proposes — `cropfarm.governor` checks the
  carry-weight and rest-interval ceilings independently and always
  escalates heavy-equipment-proximity/water-source-treatment
  decisions. Modeled on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-work-session|:approve-heavy-equipment-proximity|:approve-water-source-treatment
               :effect :propose :field-id str :carry-weight-kg number
               :hours-worked-continuous number :stake kw
               :confidence n :rationale str}"
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake field-id carry-weight-kg hours-worked-continuous] :as request}]
  {:op op
   :effect :propose
   :field-id field-id
   :carry-weight-kg carry-weight-kg
   :hours-worked-continuous hours-worked-continuous
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a crop-farm-labour advisor. Given a request, propose an
   :op, the :field-id, :carry-weight-kg and
   :hours-worked-continuous, an honest :confidence and a :stake.
   Never call an over-ceiling carry weight or an over-limit continuous
   work session conforming — the governor checks both against the
   registered field record. Heavy-equipment-proximity and water-
   source-treatment decisions always require human sign-off
   regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
