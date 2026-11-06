# Testing & Quality Assurance Protocols

This document establishes the mandatory testing protocols, execution rules, and hardware verification mandates for **Qixev FieldKit**.

---

## 1. Absolute Rules of Test Integrity

1. **Truth in Reporting**: Never claim a test passed unless the test command actually ran to completion and returned exit code `0`.
2. **No Test Weakening**: Never weaken, disable, comment out, or alter existing assertion thresholds simply to make a failing build pass. If a test fails, diagnose and fix the actual implementation bug.
3. **Report Discrepancies Transparently**: If a command exits with code `1` or higher, report the exact error, command line, and stack trace before attempting fixes.

---

## 2. Test Suite Hierarchy

The codebase maintains three levels of automated testing:

| Level | Scope | Framework | Execution Command |
| :--- | :--- | :--- | :--- |
| **Lint & Static Checks** | Code health, XML syntax, Android lint | Android Lint | `.\gradlew check` |
| **Unit Tests** | Data validators, JSON serialization, state filters, ViewModels | JUnit 4 + Coroutines Test | `.\gradlew testDebugUnitTest --rerun-tasks` |
| **Connected Tests** | Room transactions, database migrations, Compose UI, lifecycle | AndroidJUnitRunner on Hardware | `.\gradlew connectedDebugAndroidTest` |

---

## 3. Mandatory Hardware Testing Targets

### Primary Device: Redmi 7 (`df1de4f9`)
- **OS**: Android 16 (API 36, LineageOS)
- **Role**: Primary execution environment for all `connectedDebugAndroidTest` runs.
- **Verification Rule**: Any feature or bug fix modifying database logic, navigation, or Compose UI must be verified on this device before completion.

### Secondary Device: Motorola moto g(40) fusion (`ZD2223VMC2`)
- **OS**: Android 12 (API 31)
- **Role**: Regression testing for Material You wallpaper theming and Android 12 background process lifecycle.

---

## 4. Standard Verification Sequence

Every task modifying codebase logic or UI must pass the following sequence:

```powershell
# Step 1: Run static analysis and lint
.\gradlew check

# Step 2: Force rerun of all unit tests
.\gradlew testDebugUnitTest --rerun-tasks

# Step 3: Verify debug APK compilation
.\gradlew assembleDebug

# Step 4: Verify production release compilation with R8
.\gradlew assembleRelease

# Step 5: Verify connected instrumentation on physical device
.\gradlew connectedDebugAndroidTest
```

Alternatively, invoke the standardized PowerShell QA runner:
```powershell
.\scripts\qa.ps1
```

---

## 5. Investigating Test Failures

1. **Unit Test Failures**:
   - Inspect HTML reports at `app/build/reports/tests/testDebugUnitTest/index.html`.
2. **Connected Test Failures**:
   - Inspect XML test results at `app/build/outputs/androidTest-results/connected/debug/*.xml`.
   - Inspect Gradle test report at `app/build/reports/androidTests/connected/debug/index.html`.
   - Pull recent device logcat using:
     ```powershell
     adb logcat -d --pid=$(adb shell pidof -s com.qixev.qfieldkit)
     ```
3. **Database Migration Failures**:
   - Never allow fallback to destructive migration in production code or test configurations.
   - Verify table schemas, foreign key names, and index configurations match the exported Room schema JSON files.
