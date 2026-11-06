# Iteration: [Milestone / Iteration Name]

## Metadata
- Date: YYYY-MM-DD
- Version / milestone: [e.g. v0.5-b]
- Git commit: [e.g. a9e60ad]
- Git tag if applicable: [e.g. v0.5-b]
- Git state: CLEAN | DIRTY_TRACKED | DIRTY_UNTRACKED | DIRTY_STAGED | DIRTY_MIXED
- Staged changes: [e.g. 0 files]
- Tracked unstaged changes: [e.g. 1 file (M .gitignore)]
- Untracked files: [e.g. 4 entries]
- Primary device: Redmi 7 (df1de4f9, Android 16, SDK 36)
- Secondary device: Motorola moto g(40) fusion (ZD2223VMC2, Android 12, SDK 31)
- Developer/agent: Antigravity / [User]

## Objective
What were we trying to accomplish in this iteration?

## Changes
What actually changed in the codebase, resources, dependencies, or documentation?

## Validation

### Unit tests
- Result: PASS | FAIL
- Count: [e.g. 57 passed, 0 failed, 0 skipped]

### Instrumentation tests
- Result: PASS | FAIL
- Count: [e.g. 30 passed, 0 failed, 0 skipped]
- Device: Redmi 7 (df1de4f9, Android 16)

### Build
- Debug: PASS | FAIL (Artifact size)
- Release: PASS | FAIL (Artifact size, R8 minification status, signature verification)

### Static checks
- lint: PASS | FAIL
- formatting: PASS | FAIL
- git diff --check: PASS | FAIL
- other relevant checks: [e.g. Proguard verification]

## Failures & Historical Recurrence Analysis
For every failure encountered during this iteration:
1. **Failure 1: [Title]**
   - Symptom: [Exact error output or stacktrace snippet]
   - Expected behavior: [What was expected]
   - Actual behavior: [What actually happened]
   - Reproduction: [Command to reproduce]
   - Root cause: [Technical mechanism of failure]
   - Affected subsystem: [e.g. Compose UI, Room, ADB, Scripting]
   - Resolution: [How it was fixed]
   - Prevention: [Assertion, configuration, or test added]
   - Recurrence Classification: NEW | KNOWN | REGRESSION | "Insufficient historical evidence."
   - Classification Rationale: [Cite historical failure records in .qixev/failures/ or explain why evidence is insufficient. Never infer from filename alone.]

## Unexpected Work
What took longer than expected? What unexpected obstacles arose?

## Repeated Problems
Did something happen that has happened before in earlier iterations? (Reference past commits or retrospective records).

## Productivity
- Implementation time: [Estimated]
- Debugging time: [Estimated]
- Testing time: [Measured] (e.g. `scripts/qa.ps1` duration)
- Release time: [Measured] (e.g. `scripts/release.ps1` duration)
- Manual steps required: [Count & description]
- Repeated copy/paste work: [Description if applicable]

## Product Impact
- User value:
- Reliability:
- Usability:
- Performance:
- Distribution:
- Monetization potential:
*Note*: Do not invent financial results. If commercial data is unavailable, explicitly state:
> **"Not measured."**

## Lessons & Findings (Epistemic Classification)
Every conclusion MUST be categorized into one of four epistemic tiers:

### [FACT]
Directly observed, verifiable empirical data:
- [Fact 1: e.g. "All 57 unit tests and 30 instrumentation tests passed with exit code 0."]
- [Fact 2: e.g. "Release APK assembled with R8 is 1.55 MB with valid v2 signature."]

### [INFERENCE]
Logical deduction derived directly from observed facts:
- [Inference 1: e.g. "Recent documentation and script additions introduced zero regressions to core Room or Compose logic."]

### [HYPOTHESIS]
Testable conjecture requiring future empirical measurement:
- [Hypothesis 1: e.g. "Pre-verifying screen wakefulness before running connected tests will prevent 100% of daytime timeout failures."]

### [PROPOSAL]
Actionable recommendation awaiting explicit human approval:
- [Proposal 1: e.g. "Update scripts/qa.ps1 to automatically wake the device and dismiss keyguard before connected testing."]

## Proposed Improvements
List concrete, actionable improvements:

### Improvement 1: [Title]
- Problem:
- Proposed change:
- Expected benefit:
- Risk:
- Effort: Low | Medium | High
- Priority: P0 | P1 | P2 | P3

## Human Approval
- Approved: [List improvements approved by human operator]
- Rejected: [List improvements declined by human operator]
- Deferred: [List improvements postponed for future milestones]

## Permanent Rule Candidates
List prospective rules or architecture boundaries that should be considered for inclusion in `AGENTS.md` or `.agents/rules/`:
- [Candidate 1]
- [Candidate 2]

> [!IMPORTANT]
> Candidates listed here are recommendations ONLY. They MUST NOT be automatically promoted into `AGENTS.md` or `.agents/rules/` without explicit human authorization.
