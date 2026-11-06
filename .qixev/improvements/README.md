# Improvement Proposals & Lifecycle Tracking

This directory tracks structured, actionable improvement proposals for **Qixev FieldKit**.

---

## Purpose

To formulate, evaluate, and track technical, architectural, testing, and operational improvements derived from retrospectives and failure analyses.

---

## File Naming Convention

```text
YYYY-MM-DD-<improvement-slug>.md
```
Example: `2026-11-06-test-device-detection.md`

---

## Proposal Lifecycle

Every improvement proposal moves through explicit status states:

1. **Proposed**: Drafted by an agent or developer following a retrospective or failure analysis.
2. **Approved**: Evaluated and approved by human engineering leadership for implementation.
3. **Implemented**: Code or tooling changes committed to the repository.
4. **Verified**: Validated against physical hardware and automated test suites.
5. **Rejected**: Evaluated and declined due to cost, risk, or strategic misalignment.

---

## Priority Tiers

- **P0 ? Ship Blocker**: Data loss, security breach, crash on primary device, release signing failure.
- **P1 ? Important Before Next Milestone**: Test instability, edge-to-edge layout clipping, missing empty state.
- **P2 ? Worthwhile Improvement**: Test execution speedup, script ergonomics, minor memory optimization.
- **P3 ? Later / Backlog**: Cosmetic improvements, experimental refactorings, optional tooling formats.
