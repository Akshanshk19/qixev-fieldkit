# Failure: Android 16 Compose Test Desynchronization in OnboardingFlowTest

## Metadata
- Date: 2026-11-06
- Milestone: v0.5-b
- Commit: Previous test execution on branch `main`
- Environment: Windows PowerShell 5.1 / Gradle 8.11.1 / JDK 17
- Device: Redmi 7 (`df1de4f9`, Android 16, SDK 36, LineageOS)

## Symptom
`connectedDebugAndroidTest` exited with code 1 due to 1 test failure in `OnboardingFlowTest`:
`stepThree_clickCreateFirstJob_triggersCreateJobCallback` failed to locate or assert the Compose button node after multiple test runs.

## Expected Behavior
All Compose UI tests in `OnboardingFlowTest` should run sequentially in clean isolation, finding their target nodes and passing cleanly.

## Actual Behavior
`preferences_persistence_acrossInstances` ran within `OnboardingFlowTest` while using `createComposeRule()`. Because this test verified `SharedPreferences` persistence across multiple class instances without rendering a UI node tree, the underlying test Activity lifecycle was disrupted, leaving subsequent Compose assertion rules in a desynchronized state on Android 16.

## Reproduction
```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Root Cause
Mixing non-Compose `SharedPreferences` tests inside a test suite governed by `createComposeRule()` causes the test activity to enter an inactive state, triggering node lookup timeouts for subsequent tests on newer Android releases (SDK 36).

## Resolution
Separated test concerns by extracting non-UI preference tests into a dedicated instrumentation test class:
`OnboardingPreferencesAndroidTest.kt` using standard `ApplicationProvider.getApplicationContext()` without `createComposeRule()`. `OnboardingFlowTest.kt` now exclusively tests UI interactions.

## Prevention
Added clean class separation between UI interaction tests and underlying preference/data tests. Ran 30/30 connected tests cleanly on Redmi 7.

## Recurrence
- [x] **New**
First observed occurrence on Android 16 / SDK 36.

## System Improvement
Establish an automated test architecture guideline: Never place non-UI preference or repository integration tests inside classes using `createComposeRule()`.
