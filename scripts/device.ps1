<#
.SYNOPSIS
    Inspects ADB connectivity, identifies the primary development device (Redmi 7),
    and reports system & package status.
#>

[CmdletBinding()]
param(
    [string]$ExpectedSerial = "df1de4f9"
)

$ErrorActionPreference = "Stop"

# 1. Locate ADB
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
$adb = if ($adbCmd) { $adbCmd.Source } else { $null }
if (-not $adb) {
    $fallbackAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $fallbackAdb) {
        $adb = $fallbackAdb
    } else {
        Write-Error "ADB not found on PATH or in Android SDK platform-tools."
        exit 1
    }
}

Write-Host "Using ADB: $adb" -ForegroundColor Cyan

# 2. Query connected devices
$rawDevices = & $adb devices -l
Write-Host "`nConnected Devices:`n$rawDevices"

$deviceLines = $rawDevices -split "`r?`n" | Where-Object { $_ -match "^\s*([^\s]+)\s+device\b" }
if (-not $deviceLines) {
    Write-Error "No authorized ADB devices detected. Please attach the Redmi 7 and enable USB debugging."
    exit 1
}

# 3. Match Redmi 7
$foundDevice = $null
foreach ($line in $deviceLines) {
    if ($line -match "^\s*([^\s]+)\s+device\b") {
        $serial = $matches[1]
        $model = (& $adb -s $serial shell getprop ro.product.model 2>$null).Trim()
        if ($serial -eq $ExpectedSerial -or $model -match "Redmi") {
            $foundDevice = $serial
            break
        }
    }
}

if (-not $foundDevice) {
    Write-Error "Primary development device (Redmi 7, serial: $ExpectedSerial) not found among connected devices."
    exit 1
}

Write-Host "`nTarget Device Identified:" -ForegroundColor Green
Write-Host "  Serial:       $foundDevice"
$model = (& $adb -s $foundDevice shell getprop ro.product.model).Trim()
$device = (& $adb -s $foundDevice shell getprop ro.product.device).Trim()
$androidVer = (& $adb -s $foundDevice shell getprop ro.build.version.release).Trim()
$sdk = (& $adb -s $foundDevice shell getprop ro.build.version.sdk).Trim()
$display = (& $adb -s $foundDevice shell wm size).Trim()
$density = (& $adb -s $foundDevice shell wm density).Trim()

Write-Host "  Model:        $model ($device)"
Write-Host "  Android:      $androidVer (SDK $sdk)"
Write-Host "  Resolution:   $display ($density)"

# 4. Check package state
$pkg = "com.qixev.qfieldkit"
$installedRaw = & $adb -s $foundDevice shell pm list packages $pkg 2>$null
$installed = if ($installedRaw) { ($installedRaw -join " ").Trim() } else { "" }
if ($installed -match $pkg) {
    $versionInfo = (& $adb -s $foundDevice shell dumpsys package $pkg | Select-String "versionName|versionCode") -join "; "
    Write-Host "  Package:      INSTALLED ($versionInfo)" -ForegroundColor Green
} else {
    Write-Host "  Package:      NOT INSTALLED" -ForegroundColor Yellow
}

exit 0
