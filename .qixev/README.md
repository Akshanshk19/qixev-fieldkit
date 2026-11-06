# Qixev Continuous Improvement & Iteration Intelligence System

The `.qixev/` directory houses the local-first iteration memory and continuous intelligence system for **Qixev FieldKit**.

It enables autonomous and pair-programming agents to systematically learn from every development cycle by recording facts, isolating root causes, capturing metrics, and formulating evidence-based improvement proposals.

---

## 1. Directory Structure

```text
.qixev/
??? decisions/         # Architectural & Product Decision Records (ADRs)
??? experiments/       # Structured hypothesis-driven experiments
??? failures/          # Post-mortem analysis of build, test, and runtime failures
??? improvements/      # Tracked improvement proposals and lifecycle states
??? metrics/           # Engineering productivity and product performance tracking
??? retrospectives/    # Milestone and release retrospective records
??? diagnostics/       # [GIT-IGNORED] Hardware logs, crash dumps, and device traces
```

---

## 2. Core Epistemological Hierarchy

All records in this system MUST strictly distinguish between five levels of knowledge:

1. **Observed Facts**: Verifiable events, terminal outputs, exact exit codes, commit SHAs, line numbers, and device specifications. Never assume or extrapolate.
2. **Developer Conclusions**: Logical deductions drawn directly from observed facts (e.g., "The test failed because a background thread was not awaited").
3. **Hypotheses**: Testable conjectures requiring empirical verification before being accepted as fact (e.g., "Replacing reflection with compile-time code generation will reduce startup latency by 15%").
4. **Proposed Improvements**: Actionable recommendations that address observed problems. These remain proposals until reviewed and approved.
5. **Approved Permanent Rules**: Governance principles accepted through explicit human authorization and codified in `AGENTS.md` or `.agents/rules/`.

---

## 3. Approval Boundaries

The boundary between autonomous execution and human authority is absolute:

### Autonomous Capabilities (No Approval Required)
- Collecting objective build and test evidence.
- Tracking engineering metrics and durations.
- Generating retrospective drafts in `.qixev/retrospectives/`.
- Detecting failure patterns and assigning recurrence classifications (`NEW`, `KNOWN`, `REGRESSION`).
- Creating failure post-mortems in `.qixev/failures/`.
- Formulating improvement proposals in `.qixev/improvements/`.
- Documenting experiment hypotheses and results in `.qixev/experiments/`.
- Summarizing technical and procedural lessons learned.

### Human Authority Boundaries (Explicit Human Approval Required)
- Modifying `AGENTS.md` or `.agents/rules/`.
- Changing security, privacy, or network policies.
- Altering architectural boundaries or introducing dependencies.
- Modifying release or code-signing verification standards.
- Changing test pass/fail thresholds or testing policies.
- Modifying commercial, monetization, or distribution strategy.
- Promoting proposed improvements into permanent operational rules.
- Executing `git commit`, `git tag`, or `git push`.

---

## 4. File Naming Conventions

All records must use stable, chronological, lowercase kebab-case naming:
- `YYYY-MM-DD-<slug>.md` (e.g., `2026-11-06-v0.5-b.md`, `2026-11-06-android-test-failure.md`)
