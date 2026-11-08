# Qixev Agent OS ? Engineering Governance & Operational Protocol

Welcome to **Qixev FieldKit**. This document governs all autonomous and pair-programming agents operating inside this codebase. Every agent working in this repository MUST strictly abide by the rules, architecture boundaries, and verification mandates established below.

---

## 1. Core Engineering Principles

1. **Utility-First, Local-First**: FieldKit is a specialized, zero-bullshit workshop and field inventory tool. It is 100% offline. It has no cloud dependencies, no remote sync, no accounts, no telemetry, and zero network calls.
2. **Deterministic Architecture**: FieldKit follows a strict, unidirectional layering pattern:
   $$\text{Compose UI} \longrightarrow \text{ViewModel (StateFlow)} \longrightarrow \text{Repository} \longrightarrow \text{Room DAO / Entities}$$
3. **No Speculative Complexity**: Do not add DI frameworks (no Hilt, no Koin), reactive bus systems, multi-module setups, or abstract domain layers. Standard Kotlin constructor injection and the application service locator (`FieldKitApplication`) are deliberate and stable.
4. **Honesty & Truth in Verification**: Never report a test as passed unless the command ran completely and returned an exit code of `0`. Never hide compiler warnings, deprecations, or test failures. Never weaken or delete tests to force a pass.

---

## 2. Architectural Boundaries & Code Preservation

- **No Direct DAO Calls from UI or ViewModels**: UI components must never access Room DAOs, databases, or raw SQL queries directly. All data access flows through dedicated repositories (`InventoryRepository`, `RepairJobRepository`, `BackupRepository`).
- **Atomic, Transactional Data Mutations**: Every multi-entity mutation (especially stock deductions, item restitutions, and job cancellations) must execute inside a single Room `@Transaction` block (`withTransaction` / `runInTransaction`).
- **Strict Migration Safety**: Room schema migrations must be explicit and covered by automated migration tests (`DatabaseMigrationTest`). Destructive fallback (`fallbackToDestructiveMigration`) is strictly prohibited in production.
- **UI State Patterns**: ViewModels expose immutable, single `StateFlow<UiState>` models using `SharingStarted.WhileSubscribed(5_000L)` and pure functions for user intents.
- **Preferences & UI State**: App-level state flags (such as first-run onboarding completion) must live in lightweight `SharedPreferences` (`OnboardingPreferences`), never in the Room business database.

---

## 3. Design System & User Interface

- **Material 3 / Material You**: UI components use standard Material 3 with dynamic wallpaper theming enabled by default on Android 12+ (`dynamicColor = true`).
- **Calm, Premium Aesthetic**: No neon colors, cyan highlights, or rigid borders. Use tonal surfaces (`surfaceContainer`, `surfaceContainerHigh`), rounded pill search bars (`RoundedCornerShape(24.dp)`), and soft tonal circular icon badges (`72.dp`).
- **Window Insets & Status Bars**: Maintain edge-to-edge layout without double status bar insets. Root `Scaffold` must define `contentWindowInsets = WindowInsets(0, 0, 0, 0)` with child screens handling standard `TopAppBar` padding.
- **Keyboard Handling**: Activity window input mode must preserve `android:windowSoftInputMode="adjustResize|stateHidden"` to prevent the virtual keyboard from automatically popping up on screen entry.
- **Accessibility**: All interactive elements must satisfy minimum touch target guidelines ($\ge 48\times 48\text{ dp}$). Purely decorative icons must set `contentDescription = null`. Text contrast must meet Material 3 accessibility standards.

---

## 4. Hardware Verification Protocol

### Primary Reference Development & Test Device
- **Device**: **Redmi 7** (Codename: `onclite`, Lab Reference Serial: `df1de4f9`)
- **OS / Runtime**: **Android 16** (SDK 36, LineageOS)
- **Role**: Primary development and continuous automated instrumentation reference device.
- **Permissions**: This dedicated test device may be used freely by agents to clear app data (`pm clear com.qixev.qfieldkit`), install debug builds, run connected tests, inspect process states, and collect logcat. Do not touch unrelated apps or system files.
- **Device Portability**: Testing scripts also support any attached Android physical device or emulator running Android 8.0+ (API 26+) by passing `-Serial <serial>` or attaching a single device.

### Secondary Regression Device
- **Device**: **Motorola moto g(40) fusion** (Lab Reference Serial: `ZD2223VMC2`)
- **OS / Runtime**: **Android 12** (SDK 31)
- **Role**: Secondary regression target for dynamic theming, Material You wallpaper extraction, and legacy Android 12 lifecycle verification.

---

## 5. Required Verification Pipeline

Before marking any task complete or suggesting a commit, the agent MUST run the full verification pipeline:

```powershell
# 1. Static code analysis and linting
.\gradlew check

# 2. Complete unit test rerun
.\gradlew testDebugUnitTest --rerun-tasks

# 3. Debug APK build
.\gradlew assembleDebug

# 4. Production release verification (R8, shrinking, dedicated signing)
.\gradlew assembleRelease

# 5. Connected instrumentation tests on primary device (Redmi 7)
.\gradlew connectedDebugAndroidTest
```

Or run the unified agent QA script:
```powershell
.\scripts\qa.ps1
```

If any step fails, the agent must inspect the failure report, diagnose the root cause, fix the issue cleanly, and rerun the suite.

---

## 6. Security, Privacy & Release Policy

- **Zero Network Permissions**: The application manifest MUST NOT contain `<uses-permission android:name="android.permission.INTERNET" />`.
- **No Secrets in Git**: Keystores (`*.jks`, `*.keystore`), `keystore.properties`, local developer tokens, and raw diagnostic dump files (`.qixev/diagnostics/`) must remain untracked and strictly ignored.
- **Release Signing Policy**: Production release builds must only sign against a valid, dedicated release key provided via `keystore.properties`. Release builds must **never** silently fall back to the debug key.
- **Version Integrity**: Maintain explicit version metadata (`versionCode = 100`, `versionName = "1.0.0"`). Version updates require explicit milestone approval.
- **Git Safety**: Agents must never run destructive commands (`git reset --hard`, `git clean -fd`, `git push --force`) without explicit human instruction. No git commits or tags may be generated without explicit permission.

---

## 7. Continuous Improvement & Development Lifecycle

FieldKit implements an 11-stage closed-loop development and learning process:

$$\text{PLAN} \longrightarrow \text{IMPLEMENT} \longrightarrow \text{TEST} \longrightarrow \text{VERIFY} \longrightarrow \text{RELEASE} \longrightarrow \text{RETROSPECT} \longrightarrow \text{LEARN} \longrightarrow \text{PROPOSE IMPROVEMENT} \longrightarrow \text{HUMAN APPROVAL} \longrightarrow \text{UPDATE SYSTEM} \longrightarrow \text{NEXT ITERATION}$$

1. **Plan**: Establish concrete milestone scope, architectural boundaries, and test requirements.
2. **Implement**: Write minimal, utility-first code strictly adhering to layer invariants.
3. **Test**: Run local unit tests and static checks (`check`, `testDebugUnitTest`).
4. **Verify**: Run on-device connected instrumentation tests on the primary target (`Redmi 7`).
5. **Release**: Assemble hardened release APK with R8 and verify cryptographic signature.
6. **Retrospect**: Execute structured milestone retrospective via `qixev-retrospective` / `.\scripts\retrospective.ps1`.
7. **Learn**: Analyze failures, productivity, and unexpected hurdles; classify recurrence (`NEW`, `KNOWN`, `REGRESSION`).
8. **Propose Improvement**: Document concrete, prioritized P0?P3 proposals in `.qixev/improvements/`.
9. **Human Approval**: Present proposed improvements, architecture changes, and rule candidates to the human operator.
10. **Update System**: Upon explicit human authorization, codify approved changes into rules, templates, or scripts.
11. **Next Iteration**: Begin the next milestone with system-level improvements active.

---

## 8. Human Approval & Autonomous Execution Boundaries

To preserve architectural integrity and prevent unauthorized strategic drift, agents must strictly respect the boundary between autonomous actions and human authority:

### Autonomous Capabilities (No Human Approval Required)
- Collecting objective build and test evidence.
- Tracking engineering durations, APK sizes, and test metrics.
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

## 9. Operational Rules & Skills Index

Detailed operational rules and domain-specific protocols are cataloged under `.agents/rules/`:
- [Architecture Boundaries](.agents/rules/architecture.md)
- [Android & Compose Standards](.agents/rules/android.md)
- [Testing & QA Protocols](.agents/rules/testing.md)
- [Release & Signing Standards](.agents/rules/release.md)
- [Security & Data Safety](.agents/rules/security.md)
- [Git & Change Control](.agents/rules/git.md)

Dedicated agent execution skills are cataloged under `.agents/skills/`:
- `qixev-qa`: Complete automated and device testing pipeline.
- `qixev-release`: Release prerequisite audit and signature verification.
- `qixev-diagnostics`: Device inspection, crash diagnosis, and logcat extraction.
- `qixev-product-audit`: Non-invasive, prioritized evaluation of product UX and stability.
- `qixev-retrospective`: Milestone retrospective and continuous improvement protocol.

Iteration memory and historical intelligence are preserved under `.qixev/`:
- `.qixev/decisions/`: Architectural & Product Decision Records (ADRs).
- `.qixev/experiments/`: Empirical hypothesis-testing records.
- `.qixev/failures/`: Post-mortem failure analyses and recurrence tracking.
- `.qixev/improvements/`: Actionable improvement proposals and status tracking.
- `.qixev/metrics/`: Engineering productivity and product performance baselines.
- `.qixev/retrospectives/`: Milestone retrospectives and rule candidates.
