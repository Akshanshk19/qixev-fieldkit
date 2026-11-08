<#
.SYNOPSIS
    Installs the debug APK onto a target Android development device.
#>

[CmdletBinding()]
param(
    [Alias("ExpectedSerial")]
    [string]$Serial = "",
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [switch]$BuildIfMissing
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
        Write-Error "ADB not found."
        exit 1
    }
}

# 2. Resolve target device
$rawDevices = & $adb devices -l
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

$targetSerial = $null
if (-not [string]::IsNullOrWhiteSpace($Serial)) {
    if ($deviceList -contains $Serial) {
        $targetSerial = $Serial
    } else {
        Write-Error "Specified device '$Serial' is not connected or not authorized. Available devices: $($deviceList -join ', ')"
        exit 1
    }
} elseif ($deviceList.Count -eq 1) {
    $targetSerial = $deviceList[0]
} else {
    Write-Error "Multiple ADB devices detected ($($deviceList -join ', ')). Please specify a target device using -Serial <serial>."
    exit 1
}

# 3. Locate APK
$resolvedApk = Join-Path $PSScriptRoot "..\$ApkPath"
if (-not (Test-Path $resolvedApk)) {
    if ($BuildIfMissing) {
        Write-Host "Debug APK not found at $resolvedApk. Building debug APK now..." -ForegroundColor Yellow
        & .\gradlew.bat assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to build debug APK."
            exit 1
        }
    } else {
        Write-Error "APK not found at $resolvedApk. Run '.\gradlew assembleDebug' or pass -BuildIfMissing."
        exit 1
    }
}

# 4. Install APK
$model = (& $adb -s $targetSerial shell getprop ro.product.model 2>$null).Trim()
Write-Host "Installing $(Split-Path $resolvedApk -Leaf) to device $targetSerial ($model)..." -ForegroundColor Cyan
$installOutput = & $adb -s $targetSerial install -r -d $resolvedApk 2>&1
Write-Host $installOutput

if ($installOutput -match "Success") {
    Write-Host "Installation SUCCESSFUL on device $targetSerial ($model)." -ForegroundColor Green
    exit 0
} else {
    Write-Error "Installation failed on device $targetSerial."
    exit 1
}
