# Failure: Redmi 7 Screen Sleep / Dream State During Connected Compose Tests

## Metadata
- Date: 2026-11-06
- Milestone: v0.5-b / Agent OS Iteration Intelligence
- Commit: Running on branch `main`
- Environment: Windows PowerShell 5.1 / Gradle 8.11.1 / JDK 17
- Device: Redmi 7 (`df1de4f9`, Android 16, SDK 36, LineageOS)

## Symptom
`connectedDebugAndroidTest` failed with 8 test failures across all Compose UI test suites (`OnboardingFlowTest`, `JobDetailNotFoundTest`, `InventoryDeleteErrorFeedbackTest`). Data-layer Room and migration tests passed, but UI tests timed out.

## Expected Behavior
All 30 connected tests should execute sequentially and pass.

## Actual Behavior
Compose test runner could not focus the test activity or inject touch events, timing out on all Compose assertion rules.

## Reproduction
Allow the Redmi 7 to remain idle until the display enters screensaver/daydream mode (`mWakefulness=Dreaming`), then run:
```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Root Cause
The device entered ambient daydream state (`mWakefulness=Dreaming`). Under Android 16 / SDK 36, Compose instrumentation tests require an active display and focused window. When `mWakefulness` is not `Awake`, Compose fails to process UI events.

## Resolution
Executed device wake-up and keyguard dismissal via ADB:
```powershell
adb -s df1de4f9 shell input keyevent KEYCODE_WAKEUP
adb -s df1de4f9 shell wm dismiss-keyguard
adb -s df1de4f9 shell svc power stayon true
```
Rerunning `connectedDebugAndroidTest` immediately resulted in 30/30 tests passing in 31s.

## Prevention
Configured device with `svc power stayon true` so the display remains on while connected to USB power.

## Recurrence
- [x] **New**
First observed occurrence of display sleep during connected testing.

## System Improvement
Update `scripts/device.ps1` and `scripts/qa.ps1` to automatically inspect `mWakefulness` and issue `input keyevent KEYCODE_WAKEUP` and `wm dismiss-keyguard` before launching connected instrumentation tests.
