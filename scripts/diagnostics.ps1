<#
.SYNOPSIS
    Captures comprehensive device specs, app status, recent crash stacktraces,
    and logcat into a timestamped file under .qixev/diagnostics/.
#>

[CmdletBinding()]
param(
    [Alias("TargetSerial")]
    [string]$Serial = "",
    [string]$PackageName = "com.qixev.qfieldkit"
)

$ErrorActionPreference = "Continue"

# 1. Locate ADB
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
$adb = if ($adbCmd) { $adbCmd.Source } else { $null }
if (-not $adb) {
    $fallbackAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $fallbackAdb) {
        $adb = $fallbackAdb
    } else {
        Write-Error "ADB not found."
        exit 1
    }
}

# 1b. Resolve target device
$rawDevices = & $adb devices -l
$deviceList = @()
foreach ($devLine in ($rawDevices -split "`r?`n")) {
    if ($devLine -match "^\s*([^\s]+)\s+device\b") {
        $deviceList += $matches[1]
    }
}

if ($deviceList.Count -eq 0) {
    Write-Error "No authorized ADB devices detected. Please attach an Android device and enable USB debugging."
    exit 1
}

$resolvedSerial = $null
if (-not [string]::IsNullOrWhiteSpace($Serial)) {
    if ($deviceList -contains $Serial) {
        $resolvedSerial = $Serial
    } else {
        Write-Error "Specified device '$Serial' is not connected or not authorized. Available devices: $($deviceList -join ', ')"
        exit 1
    }
} elseif ($deviceList.Count -eq 1) {
    $resolvedSerial = $deviceList[0]
} else {
    Write-Error "Multiple ADB devices detected ($($deviceList -join ', ')). Please specify a target device using -Serial <serial>."
    exit 1
}
$TargetSerial = $resolvedSerial

# 2. Prepare output directory
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$diagDir = Join-Path $repoRoot ".qixev\diagnostics"
if (-not (Test-Path $diagDir)) {
    New-Item -ItemType Directory -Path $diagDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$reportFile = Join-Path $diagDir "diagnostic_$timestamp.txt"

Write-Host "Collecting diagnostics to $reportFile..." -ForegroundColor Cyan

$lines = @()
$lines += "=================================================="
$lines += "QIXEV FIELDKIT -- DEVICE DIAGNOSTIC DUMP"
$lines += "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += "Target Serial: $TargetSerial"
$lines += "Package: $PackageName"
$lines += "=================================================="

# 3. Hardware & OS info
$lines += ""
$lines += "--- [1. DEVICE SPECIFICATIONS] ---"
$model = (& $adb -s $TargetSerial shell getprop ro.product.model 2>&1)
$device = (& $adb -s $TargetSerial shell getprop ro.product.device 2>&1)
$androidVer = (& $adb -s $TargetSerial shell getprop ro.build.version.release 2>&1)
$sdk = (& $adb -s $TargetSerial shell getprop ro.build.version.sdk 2>&1)
$display = (& $adb -s $TargetSerial shell wm size 2>&1)
$density = (& $adb -s $TargetSerial shell wm density 2>&1)

$lines += "Model:        $model ($device)"
$lines += "Android:      $androidVer (SDK $sdk)"
$lines += "Display:      $display ($density)"

# 4. Package state
$lines += ""
$lines += "--- [2. PACKAGE AND PROCESS STATUS] ---"
$pmListRaw = & $adb -s $TargetSerial shell pm list packages $PackageName 2>&1
$pmList = if ($pmListRaw) { ($pmListRaw -join " ").Trim() } else { "Not installed" }
$lines += "Package Installed: $pmList"

$appPidRaw = & $adb -s $TargetSerial shell pidof -s $PackageName 2>&1
$appPid = if ($appPidRaw) { ($appPidRaw -join " ").Trim() } else { "" }
if ($appPid) {
    $lines += "Active PID: $appPid"
} else {
    $lines += "Active PID: NONE (App is not running)"
}

# 5. Crash Buffer
$lines += ""
$lines += "--- [3. LOGCAT CRASH BUFFER] ---"
$crashLog = & $adb -s $TargetSerial logcat -d -b crash 2>&1
if ($crashLog) {
    $lines += $crashLog
} else {
    $lines += "No entries found in crash buffer."
}

# 6. Filtered Fatal Exceptions & ANRs
$lines += ""
$lines += "--- [4. FATAL EXCEPTIONS AND ANR SCAN] ---"
$recentLogs = & $adb -s $TargetSerial logcat -d -t 500 2>&1
$fatalLines = $recentLogs | Select-String -Pattern "FATAL EXCEPTION", "ANR in", "AndroidRuntime" -Context 0, 10
if ($fatalLines) {
    $lines += ($fatalLines | Out-String)
} else {
    $lines += "Zero FATAL EXCEPTION or ANR patterns detected in recent 500 log lines."
}

# 7. Write to file
$lines | Out-File -FilePath $reportFile -Encoding ascii

Write-Host "Diagnostic report saved successfully: $reportFile" -ForegroundColor Green
exit 0
