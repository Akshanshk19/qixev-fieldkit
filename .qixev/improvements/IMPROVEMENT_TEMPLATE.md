# Improvement

## Metadata
- Date: YYYY-MM-DD
- Proposed By: [Name / Agent]
- Target Milestone: [e.g. v0.6-a]

## Problem
What specific bottleneck, recurring failure, architectural weakness, or UX flaw was observed?

## Evidence
Cite concrete terminal logs, line numbers, test durations, or historical failure records demonstrating the problem.

## Proposed Solution
Detailed technical description of the proposed change (code, script, architecture, configuration).

## Expected Benefit
What measurable or qualitative improvement will result? (e.g. "Reduces QA test cycle from 45s to 25s", "Guarantees Gradle never targets secondary devices").

## Cost
Estimate developer/agent effort and complexity:
- Implementation time: [e.g. 1 hour, 0.5 days]
- Complexity: Low | Medium | High

## Risk
What could break or regress if this improvement is implemented?

## Priority
- [ ] **P0** (Ship blocker)
- [ ] **P1** (Important before next milestone)
- [ ] **P2** (Worthwhile optimization)
- [ ] **P3** (Later / backlog)

## Status
- [ ] **Proposed** (Documented and awaiting review)
- [ ] **Approved** (Authorized by human operator)
- [ ] **Implemented** (Code or configuration committed)
- [ ] **Verified** (Empirically verified on hardware/tests; see Verification below)
- [ ] **Rejected** (Declined with rationale)

## Verification
How do we know the improvement actually worked?
- Validation Command:
- Success Criteria:
- Measured Evidence: [Record exact before vs. after metrics or log snippet demonstrating that the problem was eliminated or reduced]

> [!IMPORTANT]
> An improvement MUST NOT be marked as 'Verified' merely because it was proposed, approved, or implemented.
> Marking 'Verified' strictly requires empirical evidence demonstrating that the original failure or bottleneck was eliminated or reduced without introducing regressions.
