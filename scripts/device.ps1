<#
.SYNOPSIS
    Inspects ADB connectivity, identifies target Android device,
    and reports system & package status.
#>

[CmdletBinding()]
param(
    [Alias("ExpectedSerial")]
    [string]$Serial = ""
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

$deviceList = @()
foreach ($line in ($rawDevices -split "`r?`n")) {
    if ($line -match "^\s*([^\s]+)\s+device\b") {
        $deviceList += $matches[1]
    }
}

if ($deviceList.Count -eq 0) {
    Write-Error "No authorized ADB devices detected. Please attach an Android device and enable USB debugging."
    exit 1
}

# 3. Resolve target device
$foundDevice = $null
if (-not [string]::IsNullOrWhiteSpace($Serial)) {
    if ($deviceList -contains $Serial) {
        $foundDevice = $Serial
    } else {
        Write-Error "Specified device '$Serial' is not connected or not authorized. Available devices: $($deviceList -join ', ')"
        exit 1
    }
} elseif ($deviceList.Count -eq 1) {
    $foundDevice = $deviceList[0]
} else {
    Write-Error "Multiple ADB devices detected ($($deviceList -join ', ')). Please specify a target device using -Serial <serial>."
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
