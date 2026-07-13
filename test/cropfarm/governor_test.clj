(ns cropfarm.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [cropfarm.store :as store]
            [cropfarm.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Farm Crew"})
    (store/register-field! st {:field-id "F-1" :client-id "client-1"
                               :name "north-orchard"
                               :max-carry-weight-kg 25
                               :rest-break-required-after-hours 4})
    st))

(defn- session [weight hours]
  {:op :approve-work-session :effect :propose :field-id "F-1"
   :carry-weight-kg weight :hours-worked-continuous hours :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-carry-weight-and-rest-interval
  (let [st (fresh-store)
        v (governor/check req {} (session 15 2) st)]
    (is (:ok? v))))

(deftest ok-at-exact-ceilings
  (testing "both ceilings are inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (session 25 4) st)]
      (is (:ok? v)))))

(deftest hard-on-carry-weight-exceeds-ceiling
  (testing "safe carry weight is a labor-safety limit, not toughness"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (session 40 2) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :carry-weight-exceeds-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-rest-interval-exceeded
  (testing "mandated rest is arithmetic, not crew-lead discretion"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (session 15 8) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :rest-interval-exceeded (:rule %)) (:violations v))))))

(deftest hard-on-unknown-field
  (let [st (fresh-store)
        v (governor/check req {} (assoc (session 15 2) :field-id "F-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-field (:rule %)) (:violations v)))))

(deftest hard-on-foreign-field
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (session 15 2) st)]
      (is (:hard? v))
      (is (some #(= :field-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (session 15 2) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (session 15 2) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-heavy-equipment-proximity-even-at-high-confidence
  (testing "no robot operation near heavy farm equipment without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-heavy-equipment-proximity :effect :propose
                                    :field-id "F-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-water-source-treatment-even-at-high-confidence
  (testing "treatment application near water sources requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-water-source-treatment :effect :propose
                                    :field-id "F-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (session 15 2) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
