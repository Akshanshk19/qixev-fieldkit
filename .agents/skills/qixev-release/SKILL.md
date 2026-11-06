---
name: qixev-release
description: Release verification protocol for Qixev FieldKit. Audits git cleanliness, signing configuration, R8 minification, cryptographic signatures, and physical device installability. Never publishes or creates Git tags automatically.
---

# Qixev Release Verification Protocol

This skill guides agents through performing pre-release verification for **Qixev FieldKit**.

## Absolute Constraint
This skill performs **audit and verification only**. It MUST NOT:
- Publish APKs to Google Play or third-party stores.
- Create Git commits.
- Create Git tags (`git tag`).
- Push branches or tags to remote repositories.
- Modify signing keys or commit keystores.

---

## Release Verification Checklist

### 1. Verify Clean Git State
Ensure the repository state is strictly `CLEAN`:
```powershell
.\scripts\GitState.ps1
# or:
git status --porcelain
```
If the classification is `DIRTY_TRACKED`, `DIRTY_UNTRACKED`, `DIRTY_STAGED`, or `DIRTY_MIXED`, release verification MUST fail. Release builds strictly require zero uncommitted or untracked modifications.

### 2. Verify Version Metadata
Check `app/build.gradle.kts`:
- `applicationId`: Must be `com.qixev.qfieldkit`.
- `versionCode`: Positive integer (e.g., `100`).
- `versionName`: Semantic version string (e.g., `"1.0.0"`).

### 3. Verify Secret Isolation
Ensure no release credentials or keystores are tracked by Git:
```powershell
git status --ignored
```
Confirm `keystore.properties` and `*.keystore` are listed under **Ignored files**, NEVER under tracked/staged files.

### 4. Build Production Release APK
Compile the release build with R8 minification and resource shrinking:
```powershell
.\gradlew assembleRelease
```
Verify the build completes with code `0`.

### 5. Verify Artifact Generation
Ensure the APK exists and has non-zero size:
```powershell
Get-Item app/build/outputs/apk/release/app-release.apk | Select-Object Name, Length, LastWriteTime
```

### 6. Verify Cryptographic Signature
Verify that the APK is signed with a valid v2/v3/v4 signature using Android SDK's `apksigner` (if available in PATH or Android SDK):
```powershell
$apksigner = (Get-ChildItem -Path "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Filter apksigner.bat -Recurse | Select-Object -First 1).FullName
if ($apksigner) {
    & $apksigner verify --verbose app\build\outputs\apk\release\app-release.apk
}
```

### 7. Verify Installability & Launchability on Redmi 7
Install the release APK on the attached Redmi 7:
```powershell
adb -s df1de4f9 install -r app\build\outputs\apk\release\app-release.apk
```
Launch the main activity and confirm it runs without crashing:
```powershell
adb -s df1de4f9 shell am start -n com.qixev.qfieldkit/.MainActivity
Start-Sleep -Seconds 2
adb -s df1de4f9 shell pidof com.qixev.qfieldkit
```

---

## Output Report

Provide a structured release audit report:
- **Git State**: Clean / Dirty (SHA)
- **Version**: VersionCode and VersionName
- **Signing Keystore**: Verified / Missing
- **R8 Minification**: Enabled (code + resources shrunk)
- **APK Path & Size**: Path and size in MB
- **Signature Status**: Verified
- **Device Launch**: Verified on Redmi 7
- **Recommendation**: READY FOR RELEASE / NOT READY
