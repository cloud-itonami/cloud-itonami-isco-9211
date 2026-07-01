# cloud-itonami-isco-9211

Open Occupation Blueprint for **ISCO-08 9211**: Crop Farm Labourers.

This repository designs a forkable OSS business for an independent crop farm labour crew lead: a field-labour-support robot performs planting assist and harvest-carrying tasks under a governor-gated actor, so the crew keeps its own labour and safety records instead of renting a closed farm-labour SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a field-labour-support robot performs planting assist, weeding and harvest-carrying tasks under an actor that proposes
actions and an independent **Crop Farm Labour Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near heavy farm equipment, or applying treatments near water sources) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
work order + field plan + safety brief
        |
        v
Field Labour Advisor -> Crop Farm Labour Governor -> plant-support/harvest-support, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `9211`). Required capabilities:

- :robotics
- :telemetry
- :forms
- :audit-ledger
- :bpmn

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
