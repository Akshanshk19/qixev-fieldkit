---
name: qixev-diagnostics
description: Diagnostic and troubleshooting protocol for Qixev FieldKit. Inspects ADB devices, checks package status, monitors live processes, extracts filtered logcat, captures crash stacktraces, and saves reports to the local ignored diagnostics directory.
---

# Qixev Diagnostics & Troubleshooting Protocol

This skill guides agents through inspecting device state, detecting crashes, collecting logcat, and troubleshooting runtime issues on **Qixev FieldKit**.

## Diagnostic Scope

All collected logs, stacktraces, and reports must be stored under the git-ignored `.qixev/diagnostics/` directory to prevent accidental commits of device or user data.

---

## Diagnostic Procedures

### 1. Device Inspection
Check ADB server and attached devices:
```powershell
adb devices -l
```
Query core hardware and software specifications:
```powershell
adb -s df1de4f9 shell "
    echo '=== DEVICE INFO ==='
    echo 'Model: ' \$(getprop ro.product.model)
    echo 'Manufacturer: ' \$(getprop ro.product.manufacturer)
    echo 'Android Release: ' \$(getprop ro.build.version.release)
    echo 'SDK API Level: ' \$(getprop ro.build.version.sdk)
    echo 'ABI: ' \$(getprop ro.product.cpu.abi)
    echo 'Display: ' \$(wm size)
    echo 'Density: ' \$(wm density)
"
```

### 2. Package & Installation State
Verify whether `com.qixev.qfieldkit` is installed and inspect version codes:
```powershell
adb -s df1de4f9 shell pm list packages | grep com.qixev.qfieldkit
adb -s df1de4f9 shell dumpsys package com.qixev.qfieldkit | grep -E "versionCode|versionName|firstInstallTime|lastUpdateTime"
```

### 3. Process & Activity State
Check if the app is currently running:
```powershell
adb -s df1de4f9 shell pidof com.qixev.qfieldkit
```
Inspect foreground activity:
```powershell
adb -s df1de4f9 shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp"
```

### 4. Crash & Fatal Exception Detection
Search logcat buffer for recent uncaught exceptions or ANRs:
```powershell
adb -s df1de4f9 logcat -d -b crash
adb -s df1de4f9 logcat -d | grep -E -A 15 "FATAL EXCEPTION|ANR in com.qixev.qfieldkit"
```

### 5. Filtered Application Logcat
Stream or dump logs originating specifically from the FieldKit process:
```powershell
$pid = adb -s df1de4f9 shell pidof -s com.qixev.qfieldkit
if ($pid) {
    adb -s df1de4f9 logcat -d --pid=$pid
} else {
    adb -s df1de4f9 logcat -d | grep -i "qfieldkit"
}
```

### 6. Automated Diagnostics Collection
Run the unified PowerShell diagnostics script:
```powershell
.\scripts\diagnostics.ps1
```
This generates a timestamped report under `.qixev/diagnostics/report_<timestamp>.txt`.
