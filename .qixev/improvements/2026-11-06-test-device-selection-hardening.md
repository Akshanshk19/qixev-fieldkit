# Improvement: Test Device Selection and Multi-Device Hardening

## Metadata
- Date: 2026-11-06
- Proposed By: Agent OS Self-Audit
- Target Milestone: v0.6-a

## Problem
In `scripts/device.ps1` and `scripts/install.ps1`, device matching uses `$serial -eq $ExpectedSerial -or $model -match "Redmi"`. In multi-device setups where another Redmi or emulator is connected, this broad pattern can match the wrong device. Furthermore, `scripts/qa.ps1` does not set `$env:ANDROID_SERIAL` before invoking `connectedDebugAndroidTest`, causing Gradle to target all connected devices simultaneously.

## Evidence
1. `scripts/device.ps1` line 45: `if ($serial -eq $ExpectedSerial -or $model -match "Redmi")`
2. `scripts/install.ps1` line 35: `if ($serial -eq $ExpectedSerial -or $model -match "Redmi")`
3. `scripts/qa.ps1` line 91: `& .\gradlew.bat connectedDebugAndroidTest` (runs across all devices in parallel)

## Proposed Solution
1. In `device.ps1` and `install.ps1`, enforce exact serial matching first against `$ExpectedSerial`. Only fall back to model inspection if only a single device is connected.
2. In `qa.ps1`, explicitly export `$env:ANDROID_SERIAL = $ExpectedSerial` before executing connected tests.

## Expected Benefit
Guarantees deterministic execution on the designated primary test device (Redmi 7 `df1de4f9`) regardless of whether secondary devices (such as Motorola moto g40) are attached.

## Cost
- Implementation time: ~15 minutes
- Complexity: Low

## Risk
Low. Improves safety without changing application logic or dependencies.

## Priority
- [ ] P0
- [x] P1 (Important before next milestone)
- [ ] P2
- [ ] P3

## Status
- [x] Proposed
- [ ] Approved
- [ ] Implemented
- [ ] Verified
- [ ] Rejected

## Verification
- Validation Command:
  ```powershell
  # Attach both Redmi 7 and secondary device, run:
  .\scripts\qa.ps1
  ```
- Success Criteria: Connected tests execute exclusively on Redmi 7 (`df1de4f9`).
