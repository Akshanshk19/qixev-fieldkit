# Milestone & Iteration Retrospectives

This directory stores retrospective records documenting development iterations and milestone completions for **Qixev FieldKit**.

---

## Purpose

To capture the lessons, productivity metrics, failure analyses, and commercial evaluations of each development milestone, providing the continuous feedback loop for autonomous and human developers.

---

## File Naming Convention

Use the completion date and milestone identifier:
```text
YYYY-MM-DD-<milestone-or-event>.md
```
Example: `2026-11-06-v0.5-b.md`

---

## Retrospective Triggers

A structured retrospective should be executed after any of the following events:
1. Feature milestone completion (e.g. `v0.5-a`, `v0.5-b`).
2. Non-trivial bug fix requiring deep investigation.
3. Release build verification or failed release attempt.
4. Major architectural reorganization.
5. Significant UX or performance overhaul.
6. Repeated test failure or physical device crash.

---

## The Continuous Improvement Loop

```
PLAN ??> IMPLEMENT ??> TEST ??> VERIFY ??> RELEASE ??> RETROSPECT
  ?                                                       ?
  ?                                                       ?
NEXT ITERATION <?? UPDATE SYSTEM <?? HUMAN APPROVAL <?? LEARN & PROPOSE
```

---

## Epistemic Integrity Rules
- Facts must be grounded in exact test reports, exit codes, and commit SHAs.
- Productivity numbers must be explicitly labeled as `[Estimated]` or `[Measured]`.
- If commercial or user metrics are unavailable, write: `"Not measured."`
- Never automatically modify permanent governance documents (`AGENTS.md` or `.agents/rules/`). Permanent rule changes must remain candidates until reviewed and approved by a human.
