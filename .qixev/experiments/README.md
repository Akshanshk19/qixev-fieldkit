# Experiment Records

This directory records hypothesis-driven experiments conducted on **Qixev FieldKit**.

---

## Purpose

Experiments allow developers and agents to empirically test technical, architectural, or UX hypotheses before permanently integrating them into production code.

---

## File Naming Convention

```text
YYYY-MM-DD-<experiment-slug>.md
```
Example: `2026-11-06-onboarding-experiment.md`

---

## Core Rules & Epistemic Boundaries

1. **Facts vs. Assumptions**: Do NOT confuse assumptions, polite user comments, or hopes with measured results. Every experiment must define clear, objective success criteria before execution, and record measured outcomes objectively. If an experiment cannot be measured, explicitly state: `Not measured.`
2. **Strict Anti-Bias Rule**: Do NOT modify the product simply because one user requests a feature. A feature becomes a strong candidate for engineering ONLY when:
   - Multiple users independently encounter the exact same problem.
   - The problem impairs or blocks an essential workshop workflow.
   - The current product cannot reasonably handle it with existing controls.
   - The proposed solution delivers a plausible, documented business benefit.
   One-off requests must remain recorded as isolated observations unless independently validated.
3. **Product-Change Gate**: No v0.6 feature shall be implemented solely from imagination, personal preference, or a single user's request. Candidate features must reference empirical experiment evidence.

---

## Experiment Framework Documents

- [`EXPERIMENT_TEMPLATE.md`](EXPERIMENT_TEMPLATE.md): Standard hypothesis-driven technical experiment template.
- [`USER_SESSION_TEMPLATE.md`](USER_SESSION_TEMPLATE.md): Standard technician observation and interview record.
- [`OBSERVATION_PROTOCOL.md`](OBSERVATION_PROTOCOL.md): Non-leading, silent-observation field protocol.
- [`USER_INTERVIEW.md`](USER_INTERVIEW.md): Structured multi-part interview script for workflow, pain points, and pricing.
- [`RESULTS_TEMPLATE.md`](RESULTS_TEMPLATE.md): Multi-session aggregation and epistemic synthesis template.

---

## Active Experiments

- [`2026-11-06-fieldkit-user-validation.md`](2026-11-06-fieldkit-user-validation.md): **Experiment #001 — FieldKit Real-World User Validation** (Targeting 5–10 practicing repair technicians).
- [`2026-11-07-generic-work-unit-inventory-pivot.md`](2026-11-07-generic-work-unit-inventory-pivot.md): **Experiment #002 — Generic Work Units and Discrete Inventory Usage**.
