# Qixev Metrics & Engineering Tracking

This document defines the metrics catalog and tracking principles for **Qixev FieldKit**.

---

## 1. Epistemic Status of Metrics

Every reported metric MUST be explicitly marked with its epistemic status:

- **[Measured]**: Objective, empirically instrumented data (e.g. `.\gradlew check` runtime, test suite pass count, APK byte size).
- **[Estimated]**: Educated developer/agent approximation (e.g. estimated time spent debugging vs implementing). Must be labeled as an estimate.
- **[Unknown]**: Metric not currently instrumented or observed.
- **"Not measured."**: Mandatory standard text whenever a product, commercial, or telemetry metric is unavailable.

> [!CAUTION]
> Never invent or fabricate metrics. Do not fabricate user numbers, download counts, retention curves, or revenue projections. Presenting estimates or unverified data as measured facts is strictly prohibited.

---

## 2. Engineering Metrics Catalog

| Metric | Epistemic Category | Collection Method / Tool | Current Baseline (v0.5-b) |
| :--- | :--- | :--- | :--- |
| **Implementation Time** | Estimated | Developer / agent time tracking | Estimated per task |
| **Debugging Time** | Estimated | Agent trajectory / tool log timestamps | Estimated per task |
| **Testing Time** | Measured | `scripts/qa.ps1` timing | **46.7s** total suite (31.5s connected) |
| **Release Assembly Time** | Measured | `.\gradlew assembleRelease` timing | **1.5s** (cached) / **12.0s** (clean) |
| **Number of Failures** | Measured | Retrospective post-mortem count | Tracked in `.qixev/failures/` |
| **Number of Regressions** | Measured | Failures flagged as `REGRESSION` | Tracked in `.qixev/failures/` |
| **Repeated Failures** | Measured | Failures flagged as `KNOWN` | Tracked in `.qixev/failures/` |
| **Files Changed** | Measured | `git diff --stat` | Measured per commit |
| **Tests Added** | Measured | Test count delta in JUnit reports | **57** unit, **30** connected |
| **Tests Modified** | Measured | Diff on `*Test.kt` files | Measured per commit |
| **Manual Steps Required** | Measured | Count of unscripted actions | 1 (visual theme check on physical device) |
| **Automation Coverage** | Measured | Ratio of automated steps in QA | **6 / 7 steps** (85.7% automated) |

---

## 3. Product & Commercial Metrics Catalog

FieldKit is currently a 100% offline, local-first utility without cloud telemetry, accounts, or analytics SDKs.

| Metric | Status | Value / Reason |
| :--- | :--- | :--- |
| **Activation Rate** | Unknown | Not measured. (No remote analytics; zero internet permission) |
| **User Retention** | Unknown | Not measured. |
| **Feature Usage** | Unknown | Not measured. |
| **In-the-Wild Crashes** | Unknown | Not measured. (Local test device crashes captured via logcat only) |
| **Support Issues** | Unknown | Not measured. |
| **Trial-to-Paid Conversion**| Unknown | Not measured. (No payment infrastructure or paywall in v0.5) |
| **Revenue** | Unknown | Not measured. (\$0.00 / pre-monetization milestone) |

---

## 4. Hardware Baseline Performance (Redmi 7 / Android 16)

| Target Metric | Measured Value | Measurement Tool |
| :--- | :--- | :--- |
| **Cold Start to First Frame** | ~380ms | `adb shell am start -W com.qixev.qfieldkit/.MainActivity` |
| **Debug APK Size** | 7.42 MB | File length (`app-debug.apk`) |
| **Release APK Size (R8)** | 1.55 MB | File length (`app-release.apk`) |
| **Database Restore (50 items)**| < 45ms | Room `runInTransaction` in instrumentation test |
| **Connected Test Duration** | 31.5s | 30 tests via `connectedDebugAndroidTest` |
