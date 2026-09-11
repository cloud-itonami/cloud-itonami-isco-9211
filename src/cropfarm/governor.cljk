(ns cropfarm.governor
  "CropFarmLabourGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  the robot-dispensed physical work (planting assist, weeding,
  harvest-carrying) an advisor may propose. The governor never
  dispatches hardware itself. Modeled on cloud-itonami-isco-4311's
  bookkeeping.governor. Session twist: a proposed harvest-carry's
  weight is arithmetic comparison against the registered safe-lift
  ceiling — carrying beyond it is a strain-injury risk, not toughness
  — and continuous work hours are arithmetic comparison against the
  registered mandated-rest ceiling — rest intervals are arithmetic,
  not crew-lead discretion.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. field basis          — a session approval must cite a
                           REGISTERED field belonging to this client.
    4. carry-weight ceiling — the proposed carry-weight-kg must not
                           exceed the field's registered
                           :max-carry-weight-kg (a strain-injury risk,
                           not toughness).
    5. rest-interval ceiling — the proposed hours-worked-continuous
                           must not exceed the field's registered
                           :rest-break-required-after-hours (mandated
                           rest is arithmetic, not crew-lead
                           discretion).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-heavy-equipment-proximity (no robot operation near
                           heavy farm equipment without the governor
                           gate).
    7. :op :approve-water-source-treatment (treatment application
                           near water sources requires human
                           sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [cropfarm.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-heavy-equipment-proximity
                                     :approve-water-source-treatment})

(defn- hard-violations [{:keys [request proposal]} client-record f]
  (let [{:keys [op carry-weight-kg hours-worked-continuous]} proposal
        session? (= :approve-work-session op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and session? (nil? f))
      (conj {:rule :unknown-field :detail "未登録 field への作業承認は不可"})

      (and session? f (not= (:client-id f) (:client-id request)))
      (conj {:rule :field-wrong-client :detail "field が別 client のもの"})

      (and session? f (number? carry-weight-kg) (> carry-weight-kg (:max-carry-weight-kg f)))
      (conj {:rule :carry-weight-exceeds-ceiling
             :detail (str "運搬重量 " carry-weight-kg "kg > 登録済み安全上限 "
                          (:max-carry-weight-kg f) "kg（登録済み安全運搬重量超過は労災リスクであって根性の話ではない）")})

      (and session? f (number? hours-worked-continuous)
           (> hours-worked-continuous (:rest-break-required-after-hours f)))
      (conj {:rule :rest-interval-exceeded
             :detail (str "連続作業時間 " hours-worked-continuous "h > 登録済み休憩義務発生時間 "
                          (:rest-break-required-after-hours f)
                          "h（休憩義務は算術であってクルーリーダーの裁量ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `cropfarm.store/Store`. Pure — never mutates
  the store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        f (some->> (:field-id proposal) (store/field store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record f)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
