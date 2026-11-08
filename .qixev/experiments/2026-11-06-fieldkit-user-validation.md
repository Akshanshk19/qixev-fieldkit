# Experiment #001: FieldKit Real-World User Validation

## Metadata
- **Experiment ID**: EXP-001
- **Date**: 2026-11-06
- **Status**: Active (Validation Protocol Prepared)
- **Milestone Baseline**: v0.5-b / v1.0.0 (`versionCode = 100`)
- **Target Sample**: 5?10 real independent repair technicians
- **Evaluation Framework**: Epistemic separation (`[FACT]`, `[INFERENCE]`, `[HYPOTHESIS]`, `[PROPOSAL]`)

---

## 1. Hypothesis & Objective

### [HYPOTHESIS]
Small independent repair technicians will prefer FieldKit over their current manual workflow if they can handle repair intake, inventory/parts usage, job progress, and job history with less friction than their existing workflow.

### [INFERENCE]
Validating core workflows with 5?10 active repair technicians will expose critical usability friction, uncover true workflow bottlenecks, and reveal whether a local-first mobile tool provides enough commercial value to justify continued adoption or payment, preventing speculative feature engineering.

### [PROPOSAL]
Deploy the verified production release APK (`app-release.apk`, v1.0.0) to 5?10 practicing repair technicians using a standardized observation protocol and structured interview script, recording objective behavioral evidence without assisting or leading the participants.

---

## 2. Target Users

- **Cohort Size**: 5?10 independent repair technicians (electronics, phone/tablet, small appliance, or workshop repair).
- **Target Scope**: Technicians actively taking in customer repairs and managing spare parts.
- **Epistemic Constraint**:
  > [!IMPORTANT]
  > `[FACT]`: The target sample size is 5?10 real technicians.
  > `[INFERENCE]`: Do NOT claim a narrower Ideal Customer Profile (ICP) as established fact until observational session data is collected and analyzed.

---

## 3. Current Product Baseline (Verified in Repository)

All capabilities below reflect **`[FACT]`** verified through automated tests (`87/87 passed`) and physical device runs on the Redmi 7 (Android 16, SDK 36):

| Capability / Metric | Verification Status (`[FACT]`) | Implementation Detail |
| :--- | :--- | :--- |
| **Repair Jobs Lifecycle** | Verified | Intake, status progression (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`), device/customer info, labor cost. |
| **Workshop Inventory** | Verified | Item creation, SKU, category, stock count, cost/price, low-stock alerts. |
| **Parts Allocation** | Verified | Transactional deduction (`JobPartCrossRef`), stock depletion guard, parts restitution on cancellation. |
| **Job & Cost History** | Verified | Real-time status filtering, text search, dynamic parts cost aggregation, total cost calculation. |
| **First-Run Onboarding** | Verified | 3-step carousel explaining jobs & inventory, "Create First Job" CTA, persistent `SharedPreferences` suppression. |
| **Local-First & Privacy** | Verified | 100% offline, zero network permissions (`android.permission.INTERNET` absent), no cloud or tracking SDKs. |
| **Data Safety & Backup** | Verified | Versioned JSON export/restore with atomic Room transaction rollback. |
| **Inventory CSV Export** | Verified | Local spreadsheet CSV export via Android Storage Access Framework. |
| **App Version Metadata** | Verified | `versionName = "1.0.0"`, `versionCode = 100`, `minSdk = 26`, `targetSdk = 37`. |
| **Release Artifact** | Verified | `app-release.apk` (1.55 MB, R8 minified, APK Signature Scheme v2 verified). |

---

## 4. Success Signals (Objective Criteria)

Track these signals strictly as evaluation criteria, **NOT** as established results:

1. **Participation**: At least 5 practicing technicians interact with FieldKit.
2. **Autonomous Intake**: At least 5 technicians can create a complete repair job without direct verbal or physical assistance.
3. **Autonomous Parts Usage**: At least 3 technicians independently use inventory/parts functionality (adding parts to inventory or allocating them to a job).
4. **Repeat Usage**: At least 3 technicians return for another session or voluntarily use the app on a second repair job.
5. **Retention Intent**: At least 3 technicians explicitly confirm they would continue using FieldKit instead of their current system.
6. **Commercial Signal**: At least 1 technician proactively asks about pricing, paid functionality, or purchasing/licensing.

> [!CAUTION]
> **Anti-Flattery Rule**:
> Do NOT treat polite compliments such as "nice app", "looks good", or "clean UI" as validation. Only concrete behavioral completion, voluntary repeat use, and unprompted commercial inquiries constitute success evidence.

---

## 5. Failure Signals (Disconfirmation Criteria)

Record empirical evidence if participating technicians:
- Do not understand what FieldKit is for within 60 seconds of launching the app.
- Cannot complete core intake or parts allocation workflows without tester intervention.
- Express a clear preference for their existing notebook, WhatsApp, Excel, or mental workflow.
- Do not return or refuse to open the app for subsequent jobs after trying it once.
- Require constant verbal guidance to navigate screens or complete tasks.
- Identify missing operational capabilities that make practical workshop use impossible.
- Show zero willingness to pay or indicate that the tool offers no measurable economic value.
- Report that using FieldKit is noticeably slower than their existing manual methods.

---

## 6. Strict Anti-Bias Rule

> [!CRITICAL]
> **Anti-Bias Mandate**:
> Do NOT modify the product simply because one user requests a feature.
>
> A feature becomes a strong candidate for consideration ONLY when:
> 1. Multiple users independently encounter the exact same operational problem.
> 2. The problem directly blocks or impairs a critical workshop workflow.
> 3. The current product cannot reasonably accommodate the workflow with existing controls.
> 4. The proposed solution delivers a plausible, documented business or user benefit.
>
> One-off requests, personal UI preferences, or cosmetic suggestions must remain recorded as isolated observations unless independently validated across multiple participants.

---

## 7. Product-Change Gate

> [!IMPORTANT]
> **No v0.6 Feature Without Evidence**:
> No v0.6 feature shall be implemented solely from developer imagination, personal preference, or a single user's request.
>
> Every candidate feature proposed in `.qixev/improvements/` must explicitly reference empirical evidence collected via `.qixev/experiments/USER_SESSION_TEMPLATE.md` and compiled in `.qixev/experiments/RESULTS_TEMPLATE.md`.

---

## 8. Measurement & Supporting Documents

- **Observation Protocol**: [`OBSERVATION_PROTOCOL.md`](OBSERVATION_PROTOCOL.md)
- **Interview Script**: [`USER_INTERVIEW.md`](USER_INTERVIEW.md)
- **Session Evidence Template**: [`USER_SESSION_TEMPLATE.md`](USER_SESSION_TEMPLATE.md)
- **Aggregated Results Template**: [`RESULTS_TEMPLATE.md`](RESULTS_TEMPLATE.md)

---

## 9. Result & Interpretation

- **Empirical Status**: `Not measured.` (Experiment protocol prepared; live user trials in progress).
- **Interpretation**: Pending session log aggregation.

---

## 10. Decision

- [ ] **Ship**: Core hypothesis supported by success signals.
- [ ] **Revert**: Core hypothesis refuted; users reject local mobile workflow.
- [ ] **Iterate**: Critical friction identified; refine workflows based on multi-user evidence.
- [x] **Continue measuring**: Actively recruiting and observing 5?10 technicians.
