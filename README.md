# Qixev FieldKit

**Qixev FieldKit** is an offline, utility-first workshop and field inventory application for Android. It is built for independent repair technicians, maintenance engineers, field workers, and workshop managers who need fast, reliable work tracking and parts allocation without cloud dependencies, subscriptions, or accounts.

---

## Product Philosophy

- **100% Offline & Local-First**: All data is stored locally in an embedded SQLite database on your device. There is no remote synchronization, no cloud backend, and no external server dependency.
- **Zero Tracking & Total Privacy**: The application contains no analytics, no advertising, no crash telemetry, and no third-party tracking SDKs.
- **Zero Network Permissions**: The app manifest does not request or possess the Android `android.permission.INTERNET` permission. The operating system guarantees that no network transmission can occur.
- **No Mandatory Accounts**: No sign-ins, no passwords, no profiles. The tool belongs exclusively to the device.
- **Utility-First Design**: Built with Material 3 and modern Jetpack Compose for fast, calm, distraction-free operation in demanding workshop environments.

---

## Core Capabilities

- **Work Units & Project Tracking**: Create and manage jobs, builds, or repair tickets with custom titles, descriptions, and optional customer information.
- **Status Lifecycle & Integrity**: Progress tickets through *Received*, *In Progress*, *Waiting for Parts*, *Completed*, *Delivered*, and *Cancelled*. Delivered and Cancelled tickets are locked against accidental edits.
- **Workshop Inventory Catalog**: Track parts and supplies with SKU, name, category, stock count, low-stock threshold alerts, purchase cost, and selling price.
- **Discrete Inventory Usage**: Allocate parts to work units with granular metadata: quantity, timestamp, location (bench/rack), and operational purpose. Multiple allocations of the same inventory item are recorded as distinct, independent usage events.
- **Safe Restitution & Cancellations**: Removing an individual usage record safely returns only its allocated units to stock. Cancelling a work unit atomically restitutes all allocated parts back to inventory.
- **Cost & Financial Visibility**: View estimated costs, final billable totals, and real-time parts cost summaries calculated from unit selling prices at the time of allocation.
- **Data Safety & Portability**: Export full JSON database backups via the Android Storage Access Framework. Restore data with transactional validation, and export inventory lists to spreadsheet-compatible CSV files.

---

## Project Status

- **Version**: `1.0.0` (`versionCode: 100`)
- **Stage**: General Availability / Stable (`v1.0.0`)
- **Target Platform**: Android 8.0 (API level 26) through Android 16 (API level 36) (Target SDK: 37)

---

## System Requirements

- **Runtime**: Android 8.0 (API level 26) or higher.
- **Supported ABIs**: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- **Install Footprint**: ~2 MB app footprint.

---

## Building from Source

### Prerequisites
- **JDK**: Java 17 or Java 21 (Eclipse Adoptium Temurin recommended).
- **Android SDK**: Build-Tools and Platform SDK for API 37 (or configured Android SDK via `ANDROID_HOME` / `local.properties`).

### Clone & Compile Debug Build
```bash
git clone https://github.com/Akshanshk19/qixev-fieldkit.git
cd Q_FieldKit

# Build debug APK
./gradlew assembleDebug
```
The debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing Debug Build on Device
Connect an Android device with USB debugging enabled, then run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or use the included PowerShell script (Windows):
```powershell
.\scripts\install.ps1
```
*(Accepts optional `-Serial <serial>` if multiple devices are attached).*

---

## Running Automated Tests

FieldKit maintains a strict verification pipeline:

```bash
# 1. Run static analysis and lint
./gradlew check

# 2. Run unit tests
./gradlew testDebugUnitTest --rerun-tasks

# 3. Run connected instrumentation tests (requires attached device or emulator)
./gradlew connectedDebugAndroidTest
```

On Windows, the unified QA verification pipeline can be run via:
```powershell
.\scripts\qa.ps1
```

---

## Release Builds & Code Signing

FieldKit supports two distinct release build modes:

### 1. Open-Source / Contributor Build (Unsigned)
By default, compiling a release build without a signing configuration produces a minified, shrunk, unsigned release APK:
```bash
./gradlew assembleRelease
```
The unsigned release APK is output to:
`app/build/outputs/apk/release/app-release-unsigned.apk`

### 2. Production Signed Build (Qixev)
To produce a cryptographically signed release APK, create a `keystore.properties` file in the project root:
```properties
storeFile=path/to/keystore.jks
storePassword=your-keystore-password
keyAlias=your-key-alias
keyPassword=your-key-password
```
Then assemble with signing enforcement:
```bash
./gradlew assembleRelease -PrequireReleaseSigning=true
```
*(Keystore files and `keystore.properties` are strictly ignored by `.gitignore` and must never be committed to source control).*

---

## Architecture & Codebase Conventions

FieldKit follows a strict unidirectional data flow without external dependency injection frameworks:
```
Compose UI  -->  ViewModel (StateFlow)  -->  Repository  -->  Room DAO / SQLite
```
- **Local Service Locator**: `FieldKitApplication` provides singleton access to Room databases and repositories.
- **Atomic Operations**: All multi-entity modifications execute inside Room `@Transaction` blocks (`database.withTransaction`).
- **No Direct DAO Calls**: ViewModels interact exclusively with Repositories (`InventoryRepository`, `RepairJobRepository`, `BackupRepository`).

---

## Contributing

Contributions that uphold FieldKit's core principles (local-first, zero-network, zero-bloat) are welcome.

1. Ensure all proposed changes include accompanying unit and/or connected instrumentation tests.
2. Maintain clean architectural boundaries: no direct database queries from UI or ViewModels.
3. Verify the full test suite passes with zero warnings:
   ```bash
   ./gradlew check testDebugUnitTest
   ```
4. Do not introduce network permissions, tracking SDKs, or cloud services.

---

## Security & Vulnerability Reporting

Because FieldKit has zero network surface, remote attack vectors are inherently minimized. If you discover a local data safety vulnerability or security issue, please open an issue or report it privately to the maintainers before public disclosure.

---

## License

Qixev FieldKit is free and open-source software licensed under the **GNU General Public License v3.0 or later** ([GPL-3.0-or-later](LICENSE)).

Copyright © 2026 Qixev. Released under GNU GPL v3 or later.
