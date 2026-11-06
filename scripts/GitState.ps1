<#
.SYNOPSIS
    Evaluates and reports the exact Git repository status classification.
    Distinguishes: CLEAN, DIRTY_TRACKED, DIRTY_UNTRACKED, DIRTY_STAGED, DIRTY_MIXED.
    NEVER reports 'CLEAN' if any tracked, staged, or untracked changes exist.
#>

[CmdletBinding()]
param(
    [switch]$Json
)

function Get-GitRepositoryState {
    $repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

    $branch = (& git -C $repoRoot rev-parse --abbrev-ref HEAD 2>$null)
    $commit = (& git -C $repoRoot rev-parse --short HEAD 2>$null)
    $porcelain = (& git -C $repoRoot status --porcelain 2>$null)

    $stagedFiles = @()
    $trackedFiles = @()
    $untrackedFiles = @()

    if ($porcelain) {
        $lines = $porcelain -split "`r?`n"
        foreach ($line in $lines) {
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            if ($line.Length -lt 3) { continue }

            $indexChar = $line.Substring(0, 1)
            $workChar = $line.Substring(1, 1)
            $filePath = $line.Substring(3).Trim()

            if ($line.StartsWith("??")) {
                $untrackedFiles += $filePath
            } elseif ($line.StartsWith("!!")) {
                # Ignored file, skip
                continue
            } else {
                if ($indexChar -ne " " -and $indexChar -ne "?") {
                    $stagedFiles += "$indexChar $filePath"
                }
                if ($workChar -ne " " -and $workChar -ne "?") {
                    $trackedFiles += "$workChar $filePath"
                }
            }
        }
    }

    $hasStaged = ($stagedFiles.Count -gt 0)
    $hasTracked = ($trackedFiles.Count -gt 0)
    $hasUntracked = ($untrackedFiles.Count -gt 0)

    $classification = if (-not $hasStaged -and -not $hasTracked -and -not $hasUntracked) {
        "CLEAN"
    } elseif ($hasTracked -and -not $hasStaged -and -not $hasUntracked) {
        "DIRTY_TRACKED"
    } elseif ($hasUntracked -and -not $hasStaged -and -not $hasTracked) {
        "DIRTY_UNTRACKED"
    } elseif ($hasStaged -and -not $hasTracked -and -not $hasUntracked) {
        "DIRTY_STAGED"
    } else {
        "DIRTY_MIXED"
    }

    $state = [PSCustomObject]@{
        Classification = $classification
        Branch         = if ($branch) { $branch.Trim() } else { "UNKNOWN" }
        Commit         = if ($commit) { $commit.Trim() } else { "UNKNOWN" }
        IsClean        = ($classification -eq "CLEAN")
        StagedCount    = $stagedFiles.Count
        TrackedCount   = $trackedFiles.Count
        UntrackedCount = $untrackedFiles.Count
        StagedFiles    = $stagedFiles
        TrackedFiles   = $trackedFiles
        UntrackedFiles = $untrackedFiles
    }

    return $state
}

# When executed directly, print formatted output
$state = Get-GitRepositoryState

if ($Json) {
    $state | ConvertTo-Json -Depth 3
} else {
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host "QIXEV GIT STATE CLASSIFICATION" -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan

    $color = if ($state.Classification -eq "CLEAN") { "Green" } else { "Yellow" }
    Write-Host "Classification:  $($state.Classification)" -ForegroundColor $color
    Write-Host "Branch:          $($state.Branch) @ $($state.Commit)"
    Write-Host "Staged:          $($state.StagedCount) file(s)"
    Write-Host "Tracked Unstaged: $($state.TrackedCount) file(s)"
    Write-Host "Untracked:       $($state.UntrackedCount) file(s)"

    if ($state.StagedFiles.Count -gt 0) {
        Write-Host "`nStaged Files:" -ForegroundColor Yellow
        foreach ($f in $state.StagedFiles) { Write-Host "  $f" }
    }
    if ($state.TrackedFiles.Count -gt 0) {
        Write-Host "`nTracked Modified Files:" -ForegroundColor Yellow
        foreach ($f in $state.TrackedFiles) { Write-Host "  $f" }
    }
    if ($state.UntrackedFiles.Count -gt 0) {
        Write-Host "`nUntracked Files:" -ForegroundColor Gray
        foreach ($f in $state.UntrackedFiles) { Write-Host "  $f" }
    }
}
