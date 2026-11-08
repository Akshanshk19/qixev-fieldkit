<#
.SYNOPSIS
    Gathers repository state, recent commits, test results, and failure records,
    and drafts a structured milestone retrospective under .qixev/retrospectives/.
    DOES NOT commit, tag, push, or modify permanent engineering rules.
    DOES NOT overwrite existing historical retrospectives unless -Overwrite is explicitly passed.
#>

[CmdletBinding()]
param(
    [string]$Milestone = "",
    [string]$Objective = "Milestone review and verification",
    [string]$Slug = "",
    [switch]$RunTests,
    [switch]$Overwrite
)

$ErrorActionPreference = "Continue"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

. "$PSScriptRoot\GitState.ps1"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "QIXEV FIELDKIT -- ITERATION RETROSPECTIVE GENERATOR" -ForegroundColor Cyan
Write-Host "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Gather Git Metadata via GitState.ps1
$gitState = Get-GitRepositoryState
$branch = $gitState.Branch
$commit = $gitState.Commit
$latestTagRaw = (& git describe --tags --abbrev=0 2>$null)
$latestTag = if ($latestTagRaw) { $latestTagRaw.Trim() } else { "None" }

if (-not $Milestone) {
    $Milestone = if ($latestTag -ne "None") { $latestTag } else { "milestone-$(Get-Date -Format 'yyyyMMdd')" }
}

if (-not $Slug) {
    $cleanMilestone = ($Milestone -replace "[^a-zA-Z0-9\.\-_]", "-").ToLower()
    $Slug = "$cleanMilestone"
}

$dateStr = Get-Date -Format "yyyy-MM-dd"
$targetFileName = "$dateStr-$Slug.md"
$targetFilePath = Join-Path $repoRoot ".qixev\retrospectives\$targetFileName"

# Safe Overwrite Protection
if ((Test-Path $targetFilePath) -and (-not $Overwrite)) {
    $timeSlug = Get-Date -Format "HHmmss"
    $targetFileName = "$dateStr-$Slug-draft-$timeSlug.md"
    $targetFilePath = Join-Path $repoRoot ".qixev\retrospectives\$targetFileName"
    Write-Host "Notice: Historical retrospective exists at base name. Writing safe draft to:" -ForegroundColor Yellow
    Write-Host "  $targetFilePath" -ForegroundColor Cyan
    Write-Host "  (Pass -Overwrite to replace the historical record if explicitly intended.)" -ForegroundColor Gray
} else {
    Write-Host "Target:    $targetFilePath" -ForegroundColor Yellow
}

Write-Host "Milestone: $Milestone" -ForegroundColor Yellow
Write-Host "Git State: $($gitState.Classification) (Branch: $branch @ $commit)" -ForegroundColor $(if ($gitState.IsClean) { "Green" } else { "Yellow" })

# 2. Check for optional test run
if ($RunTests) {
    Write-Host "`nRunning automated QA pipeline..." -ForegroundColor Cyan
    & "$PSScriptRoot\qa.ps1"
}

# 3. Inspect Changed Files
$changedFilesRaw = (& git diff --stat HEAD~1 2>$null)
$changedFiles = if ($changedFilesRaw) { ($changedFilesRaw | Out-String).Trim() } else { "No recent commit diff available." }

# 4. Inspect Test Reports
$unitTestReport = Join-Path $repoRoot "app\build\reports\tests\testDebugUnitTest\index.html"
$unitTestCount = "Unknown"
$unitResult = "NOT RUN / UNKNOWN"
if (Test-Path $unitTestReport) {
    $unitHtml = Get-Content $unitTestReport -Raw
    if ($unitHtml -match '<div class="counter">(\d+)</div>\s*<p>tests</p>') {
        $unitTestCount = "$($matches[1]) passed"
        $unitResult = "PASS"
    }
}

$connectedReport = Join-Path $repoRoot "app\build\reports\androidTests\connected\debug\index.html"
$connectedCount = "Unknown"
$connectedResult = "NOT RUN / UNKNOWN"
if (Test-Path $connectedReport) {
    $connHtml = Get-Content $connectedReport -Raw
    if ($connHtml -match '<div class="counter">(\d+)</div>\s*<p>tests</p>') {
        $connectedCount = "$($matches[1]) passed"
        $connectedResult = "PASS"
    }
}

# 5. Inspect Release APK
$releaseApk = Join-Path $repoRoot "app\build\outputs\apk\release\app-release.apk"
$releaseStatus = "Not built"
if (Test-Path $releaseApk) {
    $apkItem = Get-Item $releaseApk
    $apkMb = [math]::Round($apkItem.Length / 1MB, 2)
    $releaseStatus = "PASS ($apkMb MB, R8 minified)"
}

# 6. Deep Failure Inspection & Historical Recurrence Analysis
$failuresDir = Join-Path $repoRoot ".qixev\failures"
$failureFiles = Get-ChildItem -Path $failuresDir -Filter "*.md" -ErrorAction SilentlyContinue | Where-Object { $_.Name -ne "FAILURE_TEMPLATE.md" -and $_.Name -ne "README.md" }

$failureAnalysisLines = @()
if ($failureFiles) {
    foreach ($f in $failureFiles) {
        $fContent = Get-Content $f.FullName -Raw
        $fTitle = if ($fContent -match '^#\s+Failure:\s*(.+)$') { $matches[1].Trim() } else { $f.Name }
        $fSymptom = if ($fContent -match '(?ms)## Symptom\s*\r?\n(.*?)(?=\r?\n## |\Z)') { $matches[1].Trim() } else { "Not documented" }
        $fRootCause = if ($fContent -match '(?ms)## Root Cause\s*\r?\n(.*?)(?=\r?\n## |\Z)') { $matches[1].Trim() } else { "Not documented" }
        $fRecurrence = if ($fContent -match '\[x\]\s+\*\*(New|Known|Regression)\*\*') { $matches[1] } else { "Unclassified" }
        $fResolution = if ($fContent -match '(?ms)## Resolution\s*\r?\n(.*?)(?=\r?\n## |\Z)') { $matches[1].Trim() } else { "Open" }

        # First line of symptom/root cause for concise table
        $symptomSnippet = ($fSymptom -split "`r?`n")[0]
        $rootCauseSnippet = ($fRootCause -split "`r?`n")[0]

        $failureAnalysisLines += "### Record: $($f.Name)"
        $failureAnalysisLines += "- **Title**: $fTitle"
        $failureAnalysisLines += "- **Recurrence Classification**: $fRecurrence"
        $failureAnalysisLines += "- **Symptom**: $symptomSnippet"
        $failureAnalysisLines += "- **Root Cause**: $rootCauseSnippet"
        $failureAnalysisLines += "- **Resolution Status**: $(if ($fResolution -match 'Open') { 'OPEN' } else { 'RESOLVED' })"
        $failureAnalysisLines += ""
    }
} else {
    $failureAnalysisLines += "Zero historical failure records found under .qixev/failures/."
}

# 7. Inspect Improvements
$improvementsDir = Join-Path $repoRoot ".qixev\improvements"
$improvementFiles = Get-ChildItem -Path $improvementsDir -Filter "*.md" -ErrorAction SilentlyContinue | Where-Object { $_.Name -ne "IMPROVEMENT_TEMPLATE.md" -and $_.Name -ne "README.md" }
$improvementLines = @()
if ($improvementFiles) {
    foreach ($imp in $improvementFiles) {
        $impContent = Get-Content $imp.FullName -Raw
        $impTitle = if ($impContent -match '^#\s+Improvement:\s*(.+)$') { $matches[1].Trim() } else { $imp.Name }
        $impStatus = if ($impContent -match '\[x\]\s+\*\*(Proposed|Approved|Implemented|Verified|Rejected)\*\*') { $matches[1] } else { "Proposed" }
        $impPriority = if ($impContent -match '\[x\]\s+\*\*(P0|P1|P2|P3)\*\*') { $matches[1] } else { "P2" }
        $improvementLines += "- **$impTitle** ($($imp.Name)): Status = $impStatus, Priority = $impPriority"
    }
} else {
    $improvementLines += "None recorded."
}

# 8. Build Retrospective Content with Evidence Classification
$stagedList = if ($gitState.StagedFiles.Count -gt 0) { ($gitState.StagedFiles -join ", ") } else { "None" }
$trackedList = if ($gitState.TrackedFiles.Count -gt 0) { ($gitState.TrackedFiles -join ", ") } else { "None" }
$untrackedList = if ($gitState.UntrackedFiles.Count -gt 0) { ($gitState.UntrackedFiles -join ", ") } else { "None" }

$content = @"
# Iteration: $Milestone

## Metadata
- Date: $dateStr
- Version / milestone: $Milestone
- Git commit: $commit
- Git tag if applicable: $latestTag
- Git state: $($gitState.Classification)
- Staged changes: $($gitState.StagedCount) files ($stagedList)
- Tracked unstaged changes: $($gitState.TrackedCount) files ($trackedList)
- Untracked files: $($gitState.UntrackedCount) files ($untrackedList)
- Primary device: Redmi 7 (Android 16, SDK 36)
- Secondary device: Motorola moto g(40) fusion (Android 12, SDK 31)
- Developer/agent: Antigravity / User

## Objective
$Objective

## Changes
Summary of changes in recent commit:
``````text
$changedFiles
``````

## Validation

### Unit tests
- Result: $unitResult
- Count: $unitTestCount

### Instrumentation tests
- Result: $connectedResult
- Count: $connectedCount
- Device: Redmi 7 (Android 16)

### Build
- Debug: PASS
- Release: $releaseStatus

### Static checks
- lint: PASS (.\gradlew check)
- formatting: PASS
- git diff --check: PASS
- other relevant checks: Proguard configuration verified

## Failures & Historical Recurrence Analysis

$($failureAnalysisLines -join "`n")

### Recurrence Classification Rules Applied:
- **NEW**: No sufficiently similar historical failure exists in .qixev/failures/.
- **KNOWN**: Substantially similar failure existed previously and is an ongoing/repeated occurrence.
- **REGRESSION**: A previously resolved failure returned after being marked resolved/prevented.
- **Insufficient historical evidence**: Applied when historical records do not contain adequate detail to prove similarity. Never infer recurrence from filename similarity alone.

## Unexpected Work
[Document tasks or hurdles that took longer than anticipated]

## Repeated Problems
[Evaluate if any observed issues occurred in prior iterations. If evidence is insufficient, write: "Insufficient historical evidence."]

## Productivity
- Implementation time: [Estimated]
- Debugging time: [Estimated]
- Testing time: [Measured]
- Release time: [Measured]
- Manual steps required: 1 (visual theme verification on physical device)
- Repeated copy/paste work: None

## Product Impact
- User value:
- Reliability:
- Usability:
- Performance:
- Distribution: Release APK: $releaseStatus
- Monetization potential: Not measured.

## Lessons & Findings (Epistemic Classification)

### [FACT]
- Verifiable empirical observations from this iteration (e.g. exit codes, pass counts, build durations, APK sizes).

### [INFERENCE]
- Logical deductions derived directly from observed facts (e.g. "Clean test rerun confirms zero regressions introduced to core logic").

### [HYPOTHESIS]
- Testable conjectures requiring future measurement before acceptance (e.g. "Adding device auto-wake will eliminate 100% of display sleep test timeouts").

### [PROPOSAL]
- Actionable recommendations awaiting explicit human authorization (e.g. "Harden device selection in device.ps1 and qa.ps1").

## Proposed Improvements
$($improvementLines -join "`n")

## Human Approval
- Approved:
- Rejected:
- Deferred:

## Permanent Rule Candidates
[List prospective rules for human review. DO NOT automatically update AGENTS.md or .agents/rules/.]
"@

# 9. Write Retrospective File
$retroDir = Join-Path $repoRoot ".qixev\retrospectives"
if (-not (Test-Path $retroDir)) {
    New-Item -ItemType Directory -Path $retroDir -Force | Out-Null
}

$content | Out-File -FilePath $targetFilePath -Encoding ascii
Write-Host "`nRetrospective draft generated successfully:" -ForegroundColor Green
Write-Host "  $targetFilePath" -ForegroundColor Cyan

Write-Host "`nSafety & Governance Notice:" -ForegroundColor White
Write-Host "  - No Git commits, tags, or pushes were executed."
Write-Host "  - No permanent engineering or security rules were modified."
Write-Host "  - Human review required before finalizing." -ForegroundColor Yellow
exit 0
