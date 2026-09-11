(ns cropfarm.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [cropfarm.actor :as actor]
            [cropfarm.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Farm Crew"})
    (store/register-field! st {:field-id "F-1" :client-id "client-1"
                               :name "north-orchard"
                               :max-carry-weight-kg 25
                               :rest-break-required-after-hours 4})
    st))

(deftest commits-an-in-limit-work-session
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-work-session :stake :low
                 :field-id "F-1" :carry-weight-kg 15 :hours-worked-continuous 2}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-carry-weight-session
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-work-session :stake :low
                 :field-id "F-1" :carry-weight-kg 50 :hours-worked-continuous 2}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-heavy-equipment-proximity-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-heavy-equipment-proximity :stake :low
                 :field-id "F-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
