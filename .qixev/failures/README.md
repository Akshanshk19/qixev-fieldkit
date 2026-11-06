# Failure Records & Post-Mortem Analysis

This directory stores structured failure post-mortems for **Qixev FieldKit**.

---

## Purpose

To systematically record build failures, test breakages, device crashes, ANRs, and regressions so that the underlying root causes are eliminated permanently.

---

## File Naming Convention

```text
YYYY-MM-DD-<failure-slug>.md
```
Example: `2026-11-06-android-test-failure.md`

---

## Pattern Detection & Recurrence Classification

Every failure record must evaluate historical evidence and assign one of three classifications:

- **NEW**: First recorded instance of this failure symptom and root cause.
- **KNOWN**: Symptom or mechanism observed in previous iterations, with documented workarounds or open improvement proposals.
- **REGRESSION**: A previously fixed defect or test failure that reappeared due to subsequent code changes.

### Strict Rule on Historical Evidence
Never claim a failure is `KNOWN` or a `REGRESSION` without referencing specific past commit SHAs, issue reports, or retrospective records.

If evidence is insufficient to establish history:
> **"Insufficient historical evidence."**
