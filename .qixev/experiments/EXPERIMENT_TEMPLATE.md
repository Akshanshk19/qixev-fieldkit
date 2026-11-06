# Experiment

## Metadata
- Date: YYYY-MM-DD
- Author: [Name / Agent]
- Status: Proposed | Running | Concluded

## Hypothesis
State the core assumption or hypothesis clearly:
"If we [implement specific change], then [specific outcome will occur]."

## Why This Matters
What problem does this address? Why is it worth validating before permanent adoption?

## Change
What exact code, asset, configuration, or architectural modification is being tested?

## Expected Result
What concrete behavioral or performance outcome is anticipated?

## Measurement
Define the objective, measurable success criteria:
- Metric: [e.g. Activity cold startup latency, APK byte size, memory heap usage]
- Measurement Tool: [e.g. ADB am start-W, apksigner, Memory Profiler]
- Target Threshold: [e.g. < 400ms, < 2.0 MB]

## Result
Recorded empirical observations:
- Measured: [Record exact numbers / logs]
- If unavailable: `Not measured.`

## Interpretation
What do the measured facts tell us about the initial hypothesis?
- Did the evidence support or refute the hypothesis?

## Decision
Select one based solely on evidence:
- [ ] **Ship**: Experiment validated; incorporate into standard codebase.
- [ ] **Revert**: Experiment refuted or caused regression; discard changes.
- [ ] **Iterate**: Results inconclusive or promising with identified flaws; refine and re-test.
- [ ] **Continue measuring**: Requires longer soak time or additional hardware verification.
