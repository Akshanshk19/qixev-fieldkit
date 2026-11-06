# Architectural & Product Decision Records

This directory contains records of significant architectural, technical, and product decisions made during the evolution of **Qixev FieldKit**.

---

## Purpose

To provide a permanent, immutable log of *why* specific technical paths were chosen, what alternatives were evaluated, and what trade-offs were accepted.

---

## File Naming Convention

Use lowercase kebab-case prefixed with the date and sequential number:
```text
YYYY-MM-DD-NNN-<topic-slug>.md
```
Example: `2026-11-06-001-local-first-shared-preferences-onboarding.md`

---

## Expected Format

Each decision record should follow this structure:

```markdown
# ADR-NNN: [Title of Decision]

## Metadata
- Date: YYYY-MM-DD
- Status: Proposed | Accepted | Deprecated | Superseded
- Author: [Name / Agent ID]
- Approved By: [Human Reviewer]

## Context & Problem Statement
What technical or business context necessitated this decision?

## Considered Options
1. Option A (Description, pros, cons)
2. Option B (Description, pros, cons)

## Decision Outcome
Chosen option and rationale.

## Positive Consequences
What improves as a result?

## Negative Consequences & Trade-offs
What risks or limitations are introduced?

## Verification
How was the validity of this decision confirmed on real hardware/code?
```
