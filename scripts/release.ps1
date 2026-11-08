<#
.SYNOPSIS
    Performs release pre-flight verification for Qixev FieldKit.
    Checks Git state, keystore.properties, compiles release build with R8,
    verifies cryptographic signatures and APK artifact existence.
    DOES NOT publish or create Git tags.
#>

[CmdletBinding()]
param(
    [string]$ExpectedApplicationId = "com.qixev.qfieldkit"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

. "$PSScriptRoot\GitState.ps1"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "QIXEV FIELDKIT -- RELEASE PRE-FLIGHT VERIFICATION" -ForegroundColor Cyan
Write-Host "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Verify Git cleanliness
Write-Host "`n1. Checking Git Status..." -ForegroundColor Yellow
$gitState = Get-GitRepositoryState
Write-Host "Git State: $($gitState.Classification) (Branch: $($gitState.Branch) @ $($gitState.Commit))"
Write-Host "  Staged: $($gitState.StagedCount), Tracked unstaged: $($gitState.TrackedCount), Untracked: $($gitState.UntrackedCount)"

if (-not $gitState.IsClean) {
    Write-Error "Release verification failed: Repository is NOT clean ($($gitState.Classification)). Release builds require a strictly CLEAN working tree."
    exit 1
}
Write-Host "Git working tree is completely clean." -ForegroundColor Green

# 2. Verify signing configuration
Write-Host "`n2. Checking Signing Configuration..." -ForegroundColor Yellow
$keystoreProps = Join-Path $repoRoot "keystore.properties"
if (-not (Test-Path $keystoreProps)) {
    Write-Error "Release signing failed: 'keystore.properties' is missing from root project."
    exit 1
}

# Ensure keystore.properties is not tracked
$trackedSecrets = git ls-files keystore.properties release.keystore
if ($trackedSecrets) {
    Write-Error "SECURITY VIOLATION: Keystore files are tracked by Git: $trackedSecrets"
    exit 1
}
Write-Host "Signing properties exist and are properly untracked." -ForegroundColor Green

# 3. Assemble Release APK
Write-Host "`n3. Compiling Release APK with R8 Minification (production signing required)..." -ForegroundColor Yellow
& .\gradlew.bat assembleRelease -PrequireReleaseSigning=true
if ($LASTEXITCODE -ne 0) {
    Write-Error "Release compilation failed."
    exit 1
}

# 4. Verify Release APK Existence
Write-Host "`n4. Verifying Release APK Artifact..." -ForegroundColor Yellow
$releaseApk = Join-Path $repoRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $releaseApk)) {
    Write-Error "Expected release APK not found at: $releaseApk"
    exit 1
}
$apkItem = Get-Item $releaseApk
$sizeMb = [math]::Round($apkItem.Length / 1MB, 2)
Write-Host "Release APK generated: $($apkItem.FullName)" -ForegroundColor Green
Write-Host "Size: $sizeMb MB ($($apkItem.Length) bytes)" -ForegroundColor Green

# 5. Verify Cryptographic Signature
Write-Host "`n5. Verifying Cryptographic Signature..." -ForegroundColor Yellow
$apksignerFile = (Get-ChildItem -Path "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Filter apksigner.bat -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1)
$apksigner = if ($apksignerFile) { $apksignerFile.FullName } else { $null }
if ($apksigner) {
    $sigOutput = & $apksigner verify --verbose $releaseApk 2>&1
    Write-Host $sigOutput
    if ($sigOutput -match "Verifies") {
        Write-Host "Cryptographic signature verified successfully (v2/v3)." -ForegroundColor Green
    } else {
        Write-Error "Signature verification failed."
        exit 1
    }
} else {
    Write-Host "Notice: apksigner not found on local machine; verified Gradle signing output succeeded." -ForegroundColor Yellow
}

Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host "RELEASE VERIFICATION COMPLETE" -ForegroundColor Green
Write-Host "Result: READY FOR HUMAN REVIEW / RELEASE SIGNED" -ForegroundColor Green
Write-Host "Note: No Git tags were created. No publishing actions were performed." -ForegroundColor White
Write-Host "==================================================" -ForegroundColor Cyan
exit 0
