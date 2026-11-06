# Release, Signing & Build Rules

This document specifies the release packaging, code shrinking, and cryptographic signing policies for **Qixev FieldKit**.

---

## 1. Versioning Standards

FieldKit uses semantic versioning enforced at the Gradle level:
- `versionCode`: Integer, monotonically incrementing (e.g., `100`).
- `versionName`: String representation of marketing version (e.g., `"1.0.0"`).
- `applicationId`: Must strictly remain `com.qixev.qfieldkit`.

Version bumps must be explicitly coordinated with milestone releases. Do not increment version codes without explicit instructions.

---

## 2. Release Signing Policy

1. **Zero Secret Tracking**:
   - Keystores (`*.jks`, `*.keystore`) and `keystore.properties` MUST NEVER be committed to Git.
   - They are ignored in `.gitignore`.
2. **Dedicated Release Key**:
   - Release signing MUST NEVER fall back to the debug key.
   - In `app/build.gradle.kts`, release builds strictly fail if `keystore.properties` is absent:
     ```kotlin
     tasks.matching { it.name == "packageRelease" || it.name == "validateSigningRelease" }.configureEach {
         if (!hasReleaseSigning) {
             doFirst {
                 throw GradleException("Production release signing is not configured...")
             }
         }
     }
     ```
3. **Local Configuration Format**:
   - Local release signing credentials live exclusively in root `keystore.properties`:
     ```properties
     storeFile=release.keystore
     storePassword=...
     keyAlias=...
     keyPassword=...
     ```

---

## 3. R8 Minification & Code Shrinking

1. **Production Hardening**:
   - Release builds enable full code and resource shrinking:
     ```kotlin
     isMinifyEnabled = true
     isShrinkResources = true
     ```
2. **ProGuard Rule Maintenance**:
   - When introducing new data models serialized to JSON (such as backup DTOs) or Room DAOs/entities, verify they are retained in `app/proguard-rules.pro`.
   - Never use wildcard `-dontwarn` rules to sweep away critical missing class warnings.

---

## 4. Release Verification Checklist

To verify release readiness without publishing or tagging:
1. Working tree is clean: `git status --porcelain` is empty.
2. Keystore and properties exist locally but are untracked: `git status --ignored`.
3. Release APK builds cleanly: `.\gradlew assembleRelease`.
4. Artifact generated: `app/build/outputs/apk/release/app-release.apk`.
5. Release signature is valid (verified via `apksigner verify` or `.\scripts\release.ps1`).
6. Release APK installs and launches cleanly on real hardware.
