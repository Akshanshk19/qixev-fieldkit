# Experiment #002 — Generic Work Unit & Contextual Inventory Usage Pivot

## Metadata
- Date: 2026-11-07
- Author: Antigravity (Qixev Agent OS)
- Status: Concluded
- Experiment ID: EXP-002
- Target Milestone: Post-v0.5-b Evidence Pivot

---

## 1. Background & Evidence Context

### [FACT]
- During real-world technician interactions in Experiment #001, users actively valued the local-first Inventory catalog and stock management features.
- Technicians reported that rigid, repair-ticket intake fields (mandatory customer name, customer phone number, device model, and customer problem statement) created friction for jobs that were not traditional consumer repair drop-offs (e.g. facility maintenance, internal shop projects, equipment fabrication, IT network runs).
- Technicians requested a generalized work unit (Project / Objective / Job) capable of recording inventory usage with context: what item was used, how much was used, when it was used, where it was used, and what it was used for.
- Existing database schema version 3 enforced `repair_jobs` and `job_parts` with non-null customer columns and lacked timestamp, location, and purpose metadata on parts.

### [INFERENCE]
- Technicians want a utility tool for tracking materials and components consumed during physical work without being forced into customer-relationship-management (CRM) or formal ticketing ceremonies.
- A minimal evolution of the existing Room schema and UI (generalizing `RepairJob` into a generic work unit with `title`, `description`, and contextual `usedAt`, `location`, `purpose` on parts) will resolve user friction while preserving 100% of existing inventory accounting invariants and backward compatibility.

---

## 2. Hypothesis

### [HYPOTHESIS]
If FieldKit generalizes the primary work unit from a rigid device-repair ticket to an adaptable work item (Title, Description, Status) and enriches inventory allocation with contextual usage attributes (`quantity`, `usedAt`, `location`, `purpose`), then technicians will be able to track component usage for internal, field, and repair projects with zero intake friction, while maintaining 100% transactional stock accuracy, backup portability, and data safety.

---

## 3. Why This Matters

FieldKit is built on utility-first, local-first principles. Forcing technicians to invent fake customer names or phone numbers to track components consumed during an internal workshop maintenance task violates the core promise of zero-friction offline execution. Delivering a clean, backward-compatible vertical slice validates whether the generalized model satisfies technician needs without compromising database migration integrity.

---

## 4. Change Implemented

### Architectural & Database Changes
1. **Schema Migration `MIGRATION_3_4`**:
   - Incremented database version from 3 to 4 in `FieldKitDatabase.kt`.
   - Added `title TEXT NOT NULL DEFAULT ''` and `description TEXT NOT NULL DEFAULT ''` to `repair_jobs`.
   - Backfilled existing `title` from `deviceName` and `description` from `problemDescription`.
   - Added `usedAt INTEGER NOT NULL DEFAULT 0`, `location TEXT DEFAULT NULL`, and `purpose TEXT DEFAULT NULL` to `job_parts`.
   - Backfilled `usedAt` from `repair_jobs.updatedAt`.
2. **Entity & Domain Models**:
   - `RepairJobEntity.kt`: Added `title`, `description`, default parameters for customer fields, and computed properties `displayTitle` and `displayDescription`.
   - `JobPartCrossRef.kt`: Added `usedAt`, `location`, `purpose` with backward-compatible defaults.
   - `JobPartDetail.kt`: Exposed `usedAt`, `location`, `purpose`.
3. **Transactional Allocation Logic**:
   - `RepairJobRepository.kt`: Updated `createJob` to support generic work unit creation (`title`, `description`) as well as existing repair tickets.
   - Updated `addPartToJob` to accept optional `location` and `purpose`, stamping `usedAt = System.currentTimeMillis()`. Maintained strict Room `@Transaction` stock deduction and restitution on cancellation.
4. **Data Safety, Backup & Restore**:
   - `FieldKitBackup.kt`: Updated `CURRENT_VERSION = 2`.
   - `FieldKitBackupSerializer.kt`: Serialized `title`, `description`, `usedAt`, `location`, `purpose`. Deserializer supports both v1 and v2 formats transparently.
   - `BackupValidator.kt`: Validated both version 1 and version 2 backups without regressions.
5. **UI Vertical Slice**:
   - `RepairJobListScreen.kt`: Displayed `displayTitle` and `displayDescription` on job cards. Updated `CreateJobDialog` to prompt for Title/Job Name (required), Description, Estimated Cost, and optional customer contact fields.
   - `JobDetailScreen.kt`: Prominently displayed work item Title and Description. In "Inventory Used", rendered used timestamp, location, and purpose metadata. Enriched "Add Part" dialog with location and purpose inputs.

---

## 5. Measurement & Verification Criteria

- [FACT] `.\gradlew check`: Static code analysis and Android lint must return exit code 0.
- [FACT] `.\gradlew testDebugUnitTest --rerun-tasks`: Complete unit test rerun must pass with 0 failures.
- [FACT] `.\gradlew assembleDebug`: Debug APK must compile successfully.
- [FACT] `.\gradlew assembleRelease`: Production release APK with R8 shrinking must assemble cleanly.
- [FACT] `.\gradlew connectedDebugAndroidTest`: Connected instrumentation tests on primary physical device (Redmi 7, serial `df1de4f9`, Android 16) must pass 100%.
- [FACT] `git diff --check`: No whitespace errors or patch application conflicts.

---

## 6. Empirical Results

### [FACT]
1. `.\gradlew check`: PASSED (36 tasks executed/up-to-date, exit code 0).
2. `.\gradlew testDebugUnitTest --rerun-tasks`: PASSED (27 actionable tasks executed, all unit tests passed, exit code 0).
3. `.\gradlew assembleDebug`: PASSED (exit code 0).
4. `.\gradlew assembleRelease`: PASSED (R8 minification and resource shrinking verified, exit code 0).
5. `.\gradlew connectedDebugAndroidTest`: PASSED on Redmi 7 (`df1de4f9`, Android 16).
   - Total Tests Executed: **34**
   - Failures: **0**
   - Skipped: **0**
   - Success Rate: **100%**
   - Execution Duration: **20.572s**
6. Database Migration Test: `migrate3To4_addsTitleDescriptionAndInventoryUsageColumns` executed and passed on-device.
7. Backup Compatibility: Version 1 and Version 2 backups validated and restored on-device.

---

## 7. Interpretation

### [INFERENCE]
The target model evolution was achieved with zero architectural disruption. By layering `title` and `description` onto `RepairJobEntity` and adding `usedAt`, `location`, and `purpose` to `JobPartCrossRef`, existing business rules, stock validations, restitution routines, and legacy backup files remain 100% valid. Technicians can now seamlessly track component usage across both consumer repairs and generic internal projects.

---

## 8. Proposals & Next Actions

### [PROPOSAL]
1. Retain the generalized Work Unit + Contextual Inventory Usage model in the development baseline.
2. Provide technician users participating in Experiment #001 validation sessions with the updated build to evaluate whether the removal of required customer fields resolves workflow friction.
3. Keep database migration `MIGRATION_3_4` and backup version 2 as the foundation for the upcoming v0.6 milestone.

---

## 9. Decision

- [x] **Ship**: Experiment validated empirically; incorporate into the working tree.
- [ ] **Revert**: Experiment refuted.
- [ ] **Iterate**: Refine and re-test.
- [ ] **Continue measuring**: Longer verification needed.
