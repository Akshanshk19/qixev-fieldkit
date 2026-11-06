---
name: qixev-qa
description: Complete quality assurance and device verification protocol for Qixev FieldKit. Guides agents through running lint, unit tests, debug builds, physical device installation, and connected instrumentation tests on the Redmi 7 test device.
---

# Qixev QA Protocol & Execution Workflow

This skill guides agents through executing the complete Quality Assurance (QA) verification pipeline on **Qixev FieldKit**.

## Core Verification Rules

1. **Never Assume**: Never report a test as passed unless the terminal command executed to completion and returned exit code `0`.
2. **Redmi 7 Primary Device**: The physical Redmi 7 (`df1de4f9`, Android 16, SDK 36) is the primary target for connected tests and live app runs.
3. **Transparent Reporting**: Include exact command invocations, exit codes, test counts, and XML/log snippets in the final report.

---

## QA Execution Steps

### Step 1: Inspect Git State
Confirm working tree status, active branch, and classify repository state:
```powershell
.\scripts\GitState.ps1
# or:
git status
git log -n 1 --oneline
```
Classify repository state explicitly as: `CLEAN`, `DIRTY_TRACKED`, `DIRTY_UNTRACKED`, `DIRTY_STAGED`, or `DIRTY_MIXED`.
Never report the repository as "clean" if any tracked, staged, or untracked changes exist.

### Step 2: Verify Device Connectivity
Confirm that the primary development device is attached and authorized:
```powershell
.\scripts\device.ps1
# or:
adb devices -l
adb -s df1de4f9 shell getprop ro.product.model
```
If the device is not attached, report the failure immediately. Do not attempt connected tests without an active device.

### Step 3: Run Static Analysis & Lint
```powershell
.\gradlew check
```
Inspect `app/build/reports/lint-results-debug.html` if warnings or errors occur.

### Step 4: Run Unit Tests
Force a clean execution of all unit tests:
```powershell
.\gradlew testDebugUnitTest --rerun-tasks
```
Verify the test count and zero failures by inspecting `app/build/reports/tests/testDebugUnitTest/index.html`.

### Step 5: Build Debug APK
```powershell
.\gradlew assembleDebug
```
Confirm the binary is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Step 6: Install on Redmi 7
```powershell
adb -s df1de4f9 install -r -d app\build\outputs\apk\debug\app-debug.apk
```
Confirm `Success` is returned.

### Step 7: Clean App Data (When Testing Clean First-Run)
When fresh state verification is needed:
```powershell
adb -s df1de4f9 shell pm clear com.qixev.qfieldkit
```

### Step 8: Launch Application
```powershell
adb -s df1de4f9 shell am start -n com.qixev.qfieldkit/.MainActivity
```
Confirm the process starts:
```powershell
adb -s df1de4f9 shell pidof com.qixev.qfieldkit
```

### Step 9: Run Connected Instrumentation Tests
```powershell
.\gradlew connectedDebugAndroidTest
```
Inspect the output XML report at:
`app/build/outputs/androidTest-results/connected/debug/TEST-Redmi 7 - 16.xml`
Verify `<testsuites tests="..." failures="0" errors="0" skipped="0">`.

### Step 10: Collect Diagnostics on Failure
If any test or app launch fails, immediately collect logcat and crash dumps:
```powershell
.\scripts\diagnostics.ps1
```
Or manually extract relevant logs:
```powershell
adb -s df1de4f9 logcat -d --pid=$(adb -s df1de4f9 shell pidof -s com.qixev.qfieldkit)
```

---

## Summary Format

Always return a clear table summarizing:
- Git status & SHA
- Device model, serial, and Android version
- Lint result (`check`)
- Unit test result count and duration
- Debug build status
- Connected test result count and duration
- Status: **PASSED** or **FAILED**
