# Security, Privacy & Data Safety Rules

This document specifies the privacy guarantees, data boundary rules, and permission limits for **Qixev FieldKit**.

---

## 1. Zero Network & Total Offline Guarantee

1. **No Internet Permission**:
   - The Android Manifest (`app/src/main/AndroidManifest.xml`) must NEVER declare:
     ```xml
     <uses-permission android:name="android.permission.INTERNET" />
     ```
   - FieldKit does not communicate with external servers, cloud providers, analytics suites, crash collectors, or ad networks.
2. **Zero Third-Party SDKs**:
   - Do not include Firebase, Google Play Services, Mixpanel, Sentry, Facebook SDK, or any other telemetry/ad SDKs.

---

## 2. Secrets & Credential Protection

1. **Git Repository Hygiene**:
   - No private keys, passwords, certificates, or keystore files may be checked into version control.
   - The `.gitignore` must continuously ignore `*.jks`, `*.keystore`, and `keystore.properties`.
   - Generated diagnostics, device dumps, and logcat captures must be saved under `.qixev/diagnostics/` (which is git-ignored).
   - Iteration memory records (`.qixev/decisions/`, `.qixev/retrospectives/`, `.qixev/failures/`, `.qixev/improvements/`, `.qixev/metrics/`) must never contain private customer data, proprietary keys, or device secrets.

---

## 3. Data Safety & Backup Integrity

1. **Deterministic JSON Backup**:
   - Backups must use an explicitly versioned format (`backupVersion = 1`).
   - Fields must be serialized with deterministic keys (`BackupSerializer.kt`).
2. **Atomic Restores with Automatic Rollback**:
   - The restore flow (`BackupRepository.kt`) must validate file integrity and schema version prior to mutating any table.
   - Restores must execute inside a Room database transaction. If any record insertion fails or violates constraints, the entire restore must roll back automatically, leaving existing database records intact.
3. **CSV Export Privacy**:
   - CSV exports (`CsvExporter.kt`) must only include workshop inventory data explicitly requested by the user, formatted for local spreadsheet use.

---

## 4. Android Process & Sandboxing

1. **Scoped Storage Compliance**:
   - Backup files and CSV exports are written using Android's Storage Access Framework (`ActivityResultContracts.CreateDocument` / `OpenDocument`).
   - The application does not require broad storage permissions (`MANAGE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE`).
2. **App-Private Storage**:
   - The SQLite database and `SharedPreferences` reside strictly within `/data/data/com.qixev.qfieldkit/`.
