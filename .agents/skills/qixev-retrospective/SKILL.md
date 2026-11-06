---
name: qixev-retrospective
description: Continuous improvement and iteration retrospective protocol for Qixev FieldKit. Guides agents through structured milestone reviews, failure analysis, metrics collection, and improvement proposal generation without modifying permanent engineering rules.
---

# Qixev Milestone Retrospective Protocol

This skill guides autonomous and pair-programming agents through conducting structured retrospectives after meaningful development milestones on **Qixev FieldKit**.

---

## 1. Trigger Conditions

Execute this protocol whenever one of the following milestone events occurs:
1. **Feature Completion**: Delivery of a milestone feature (e.g. `v0.5-b`).
2. **Non-Trivial Bug Fix**: Resolution of a defect that required debugging across multiple layers.
3. **Release Execution**: Completion of an `assembleRelease` verification or attempted production build.
4. **Build / Test Failure**: Any failed release or test regression on physical hardware.
5. **Major Architecture Change**: Alteration to database schema, repository flow, or navigation structure.
6. **Significant UX Overhaul**: Theming, layout redesign, or onboarding flow modification.
7. **Production / Runtime Issue**: Physical device crash, ANR, or data serialization error.

---

## 2. Retrospective Execution Steps

### Step 1: Inspect Git History & Active State
Collect commit history, active branch, and classify repository state:
```powershell
.\scripts\GitState.ps1
# or:
git status --porcelain
git log -n 5 --oneline
```
Classify repository state explicitly as: `CLEAN`, `DIRTY_TRACKED`, `DIRTY_UNTRACKED`, `DIRTY_STAGED`, or `DIRTY_MIXED`. Never report the working tree as clean if any change exists.

### Step 2: Inspect Changed Files & Diff Scope
Analyze the scope of changes:
```powershell
git diff --stat HEAD~1
```
Count files added, modified, or deleted.

### Step 3: Inspect Automated Test & Build Reports
Review the latest test outcomes:
- Unit test HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`
- Connected test XML reports: `app/build/outputs/androidTest-results/connected/debug/`
- Release APK artifact: `app/build/outputs/apk/release/app-release.apk`

Or execute the QA pipeline to gather fresh metrics:
```powershell
.\scripts\qa.ps1
```

### Step 4: Analyze Failures & Recurrence
For any failure observed during the cycle:
1. Review crash or error traces.
2. Determine root cause and resolution.
3. Compare against existing records in `.qixev/failures/`:
   - Inspect failure symptoms, root causes, affected subsystems, device environments, resolutions, and preventions.
   - **Never infer recurrence from filename similarity alone.**
   - Mark as **NEW** if no sufficiently similar historical failure exists.
   - Mark as **KNOWN** if substantially similar failure existed previously and is an ongoing issue.
   - Mark as **REGRESSION** if a previously resolved failure returned after being marked resolved/prevented.
   - If historical records do not provide adequate evidence to establish similarity, explicitly write:
     > **"Insufficient historical evidence."**
4. If the failure was non-trivial, create a failure post-mortem in `.qixev/failures/YYYY-MM-DD-<slug>.md`.

### Step 5: Capture Productivity & Product Metrics (Epistemic Classification)
1. Record test suite durations, build times, and APK sizes.
2. Categorize all findings under explicit epistemic tiers:
   - **[FACT]**: Directly observed, verifiable empirical data (exit codes, timings, pass counts).
   - **[INFERENCE]**: Logical conclusions derived directly from observed facts.
   - **[HYPOTHESIS]**: Testable conjectures requiring future measurement.
   - **[PROPOSAL]**: Actionable recommendations awaiting explicit human authorization.
   - Never present hypotheses as measured outcomes or proposals as implemented improvements.
3. Evaluate product impact dimensions:
   - User value, reliability, usability, performance, distribution, monetization potential.
   - If commercial or user metrics are unavailable, write: `"Not measured."`
   - Never invent or fabricate financial/user data.

### Step 6: Formulate Concrete Improvements
Identify actionable improvements addressing observed bottlenecks:
- Problem, proposed change, expected benefit, risk, effort, priority (P0?P3).
- Note: Improvements must remain in `Proposed` state until approved and cannot be marked `Verified` without documented empirical evidence that the problem was eliminated.
- Create proposal records in `.qixev/improvements/YYYY-MM-DD-<slug>.md` if significant.

### Step 7: Draft Retrospective Document
Generate the completed retrospective markdown file:
- Target path: `.qixev/retrospectives/YYYY-MM-DD-<milestone>.md`
- Use `.qixev/retrospectives/RETROSPECTIVE_TEMPLATE.md` as the template.
- **Do not overwrite historical retrospectives.** If a record already exists, write to a timestamped draft (`-draft-<timestamp>.md`) unless `-Overwrite` is explicitly authorized.

Alternatively, execute the automated retrospective script:
```powershell
.\scripts\retrospective.ps1 -Milestone "v0.5-b"
```

---

## 3. Strict Authority Boundaries

> [!CAUTION]
> The retrospective protocol is an observational and proposal mechanism.
> It MUST NEVER:
> - Automatically modify `AGENTS.md` or `.agents/rules/`.
> - Automatically alter security, privacy, or network policies.
> - Automatically commit, tag, or push to Git.
>
> All proposed permanent rule changes must remain listed under **Permanent Rule Candidates** in the retrospective file until explicitly approved and enacted by a human developer.
