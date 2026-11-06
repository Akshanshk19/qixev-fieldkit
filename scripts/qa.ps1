<#
.SYNOPSIS
    Runs the complete Qixev QA verification pipeline:
    Git status -> Gradle check -> Unit tests -> Debug build -> Device detect -> Install -> Connected tests.
#>

[CmdletBinding()]
param(
    [string]$ExpectedSerial = "df1de4f9",
    [switch]$RequireCleanGit
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

. "$PSScriptRoot\GitState.ps1"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "QIXEV FIELDKIT -- AUTOMATED QA PIPELINE" -ForegroundColor Cyan
Write-Host "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$results = [ordered]@{}

# Helper function to run a step
function Invoke-Step {
    param(
        [string]$StepName,
        [scriptblock]$Action
    )

    Write-Host "`n>>> [RUNNING] $StepName..." -ForegroundColor Yellow
    $startTime = Get-Date
    try {
        & $Action
        $duration = ((Get-Date) - $startTime).TotalSeconds
        $results[$StepName] = "PASS ($([math]::Round($duration, 1))s)"
        Write-Host ">>> [PASS] $StepName ($([math]::Round($duration, 1))s)" -ForegroundColor Green
        return $true
    } catch {
        $duration = ((Get-Date) - $startTime).TotalSeconds
        $results[$StepName] = "FAIL ($([math]::Round($duration, 1))s)"
        Write-Host ">>> [FAIL] ${StepName}: $_" -ForegroundColor Red
        return $false
    }
}

# 1. Git Status
$gitClean = Invoke-Step "1. Git State & Branch Check" {
    $state = Get-GitRepositoryState
    Write-Host "Git State: $($state.Classification) (Branch: $($state.Branch) @ $($state.Commit))"
    Write-Host "  Staged: $($state.StagedCount), Tracked unstaged: $($state.TrackedCount), Untracked: $($state.UntrackedCount)"
    if (-not $state.IsClean) {
        Write-Host "Repository is NOT clean ($($state.Classification)). Modifications present." -ForegroundColor Yellow
        if ($RequireCleanGit) {
            throw "Repository is not clean ($($state.Classification)). Aborting QA due to -RequireCleanGit."
        }
    } else {
        Write-Host "Repository is completely clean." -ForegroundColor Green
    }
}

# 2. Gradle Check
$checkPassed = Invoke-Step "2. Gradle Check & Static Lint" {
    & .\gradlew.bat check
    if ($LASTEXITCODE -ne 0) { throw "gradlew check failed with exit code $LASTEXITCODE" }
}

# 3. Unit Tests
$unitPassed = Invoke-Step "3. Unit Tests Rerun" {
    & .\gradlew.bat testDebugUnitTest --rerun-tasks
    if ($LASTEXITCODE -ne 0) { throw "testDebugUnitTest failed with exit code $LASTEXITCODE" }
}

# 4. Debug Build
$buildPassed = Invoke-Step "4. Debug APK Assembly" {
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed with exit code $LASTEXITCODE" }
}

# 5. Detect Redmi 7
$devicePassed = Invoke-Step "5. Detect Redmi 7 Device" {
    & "$PSScriptRoot\device.ps1" -ExpectedSerial $ExpectedSerial
    if ($LASTEXITCODE -ne 0) { throw "Device detection failed." }
}

# 6. Install Debug APK
$installPassed = Invoke-Step "6. Install Debug APK on Device" {
    & "$PSScriptRoot\install.ps1" -ExpectedSerial $ExpectedSerial
    if ($LASTEXITCODE -ne 0) { throw "Installation failed." }
}

# 7. Connected Instrumentation Tests
$connectedPassed = Invoke-Step "7. Connected Instrumentation Tests" {
    $env:ANDROID_SERIAL = $ExpectedSerial
    # Ensure test device is awake and unlocked (prevention for doze/daydream failure)
    & adb -s $ExpectedSerial shell input keyevent KEYCODE_WAKEUP 2>$null
    & adb -s $ExpectedSerial shell wm dismiss-keyguard 2>$null
    & .\gradlew.bat connectedDebugAndroidTest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Collecting crash diagnostics due to test failure..." -ForegroundColor Red
        & "$PSScriptRoot\diagnostics.ps1" -TargetSerial $ExpectedSerial
        throw "connectedDebugAndroidTest failed with exit code $LASTEXITCODE"
    }
}

# Summary Report
Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host "QA PIPELINE EXECUTION SUMMARY" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

Write-Host "`n[A. AUTOMATED VERIFICATION RESULTS]" -ForegroundColor White
foreach ($k in $results.Keys) {
    $color = if ($results[$k] -match "PASS") { "Green" } else { "Red" }
    Write-Host "  $($k.PadRight(40)) : $($results[$k])" -ForegroundColor $color
}

Write-Host "`n[B. DEVICE / HARDWARE TEST TARGET]" -ForegroundColor White
Write-Host "  Target Device : Redmi 7 ($ExpectedSerial)"
Write-Host "  Platform      : Android 16 (SDK 36)"
Write-Host "  Test Harness  : AndroidJUnitRunner / connectedDebugAndroidTest"

Write-Host "`n[C. MANUAL VERIFICATION STATUS]" -ForegroundColor White
Write-Host "  Manual UI / Exploratory Verification: REQUIRED BY HUMAN OPERATOR" -ForegroundColor Yellow
Write-Host "  (Automated QA verifies compilation, unit logic, and instrumentation rules."
Write-Host "   Manual visual check of dynamic theme & layout spacing on device remains mandatory.)"

$anyFailed = $results.Values | Where-Object { $_ -match "FAIL" }
if ($anyFailed) {
    Write-Host "`nOVERALL STATUS: FAILED" -ForegroundColor Red
    exit 1
} else {
    Write-Host "`nOVERALL STATUS: PASSED" -ForegroundColor Green
    exit 0
}
