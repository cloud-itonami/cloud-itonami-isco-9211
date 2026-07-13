(ns cropfarm.store
  "SSoT for the ISCO-08 9211 independent crop farm labour practice
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a field-labour-support robot
  performs planting assist, weeding and harvest-carrying tasks under
  this advisor/governor pair, which never dispatches hardware
  itself). Modeled on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    field  — a registered field {:field-id :client-id :name
             :max-carry-weight-kg number
             :rest-break-required-after-hours number}.
             `:max-carry-weight-kg` is the registered ceiling a
             proposed harvest-carry's weight must not exceed —
             carrying beyond the registered safe-lift weight is a
             strain-injury risk, not toughness;
             `:rest-break-required-after-hours` is the registered
             ceiling a proposed work session's continuous hours must
             not exceed — mandated rest intervals are arithmetic, not
             crew-lead discretion.
    record — a committed operating record (approved work session) —
             written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (field [s field-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-field! [s f])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (field [_ field-id] (get-in @a [:fields field-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-field! [s f]
    (swap! a assoc-in [:fields (:field-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :fields {} :records [] :ledger []}
                                   seed)))))
