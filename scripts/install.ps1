<#
.SYNOPSIS
    Installs the debug APK onto the dedicated Redmi 7 development device.
#>

[CmdletBinding()]
param(
    [string]$ExpectedSerial = "df1de4f9",
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

# 2. Verify target device is Redmi 7
$rawDevices = & $adb devices -l
$isRedmiConnected = $false
foreach ($line in ($rawDevices -split "`r?`n")) {
    if ($line -match "^\s*([^\s]+)\s+device\b") {
        $serial = $matches[1]
        $model = (& $adb -s $serial shell getprop ro.product.model 2>$null).Trim()
        if ($serial -eq $ExpectedSerial -or $model -match "Redmi") {
            $isRedmiConnected = $true
            $targetSerial = $serial
            break
        }
    }
}

if (-not $isRedmiConnected) {
    Write-Error "Safety restriction: Target device must be Redmi 7 ($ExpectedSerial). Refusing to install on unknown device."
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

Write-Host "Installing $(Split-Path $resolvedApk -Leaf) to Redmi 7 ($targetSerial)..." -ForegroundColor Cyan
$installOutput = & $adb -s $targetSerial install -r -d $resolvedApk 2>&1
Write-Host $installOutput

if ($installOutput -match "Success") {
    Write-Host "Installation SUCCESSFUL on Redmi 7 ($targetSerial)." -ForegroundColor Green
    exit 0
} else {
    Write-Error "Installation FAILED: $installOutput"
    exit 1
}
