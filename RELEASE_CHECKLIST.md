# Qixev FieldKit — Release Pre-Flight Checklist

This checklist governs the formal release candidate verification and distribution sign-off for **Qixev FieldKit** (`com.qixev.qfieldkit`).

---

## 1. Governance & Environmental Safety
- [x] **Zero Network Surface**: Application manifest contains no `<uses-permission android:name="android.permission.INTERNET" />`.
- [x] **Zero Secret Tracking**: `keystore.properties` and release keystores (`*.keystore`, `*.jks`) remain uncommitted and untracked.
- [x] **Working Tree Cleanliness**: `.\scripts\GitState.ps1` confirms working tree state without uncommitted or unexpected files before tagging.
- [x] **No Third-Party Analytics/Telemetry**: App operates 100% offline with no external SDK dependencies or data beacons.

---

## 2. Code Quality & Automated Test Gates
- [x] **Unit Test Suite**: All local unit tests run and pass cleanly:
  ```powershell
  .\gradlew testDebugUnitTest --rerun-tasks
  ```
- [x] **Static Analysis & Linting**: Android lint runs with 0 fatal errors:
  ```powershell
  .\gradlew check
  ```
- [x] **Connected Hardware Instrumentation**: 100% pass rate on physical reference device (Redmi 7 / Android 16):
  ```powershell
  .\gradlew connectedDebugAndroidTest
  ```
  *(Verified: 39 tests executed, 0 failures, 0 skipped, 100% success rate)*.

---

## 3. Database Invariants & Migration Safety
- [x] **Referential Integrity**: All Room foreign keys (`job_parts -> inventory_items` RESTRICT, `job_parts -> repair_jobs` CASCADE) enforced and tested.
- [x] **Discrete Usage Events**: Auto-incrementing primary key on `JobPartCrossRef` validated for repeated usages of the same part within a single work unit.
- [x] **Migrations Verified**: Schema migrations 1->2, 2->3, and 3->4 verified via automated migration tests (`DatabaseMigrationTest`).
- [x] **Transactional Integrity**: Multi-table modifications (part allocation, stock restitution, job cancellation, backup restoration) wrapped in `database.withTransaction`.

---

## 4. Release Build & Packaging
- [x] **Code & Resource Shrinking**: R8 minification enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
- [x] **Release Keystore Signing**: Production signing configured and verified using dedicated key (`keystore.properties`).
- [x] **Non-Debuggable**: Release manifest verified with `android:debuggable="false"`.
- [x] **Signed Release APK**:
  - Path: `app/build/outputs/apk/release/app-release.apk`
  - Size: ~1.56 MB
  - Signature Scheme: v2 verified via `apksigner`
- [x] **Signed Release AAB (Android App Bundle)**:
  - Path: `app/build/outputs/bundle/release/app-release.aab`
  - Size: ~3.78 MB (3,962,401 bytes)
  - Build Task: `.\gradlew bundleRelease`
  - Signature: Verified via `jarsigner` (`CN=Qixev, OU=Qixev, O=Qixev, L=Noida, ST=Uttar Pradesh, C=IN`)

---

## 5. Physical Device Smoke Validation
- [x] **Clean Installation**: APK installs cleanly via `adb install -r`.
- [x] **First-Run Onboarding**: 3-step walkthrough functions properly; skip and completion persist in `SharedPreferences`.
- [x] **Core Work Unit Lifecycle**: Create work unit, edit details, progress through status lifecycle, cancel with full stock restitution.
- [x] **Repeated Usage Verification**: Add identical inventory item multiple times with distinct timestamps, locations, and purposes; verify independent records and discrete deletion.
- [x] **Backup & Restore**: Export full JSON backup via Storage Access Framework, restore into fresh state, verify referential and data survival.

---

## 6. Distribution Sign-Off
- **Version Name**: `1.0.0`
- **Version Code**: `100`
- **Application ID**: `com.qixev.qfieldkit`
- **Target SDK**: `37`
- **Min SDK**: `26`
- **Status**: Ready for Human Approval and Tagging.
