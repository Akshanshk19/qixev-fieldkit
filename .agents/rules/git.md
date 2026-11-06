# Git & Change Control Protocol

This document governs Git operations, branch strategies, commit conventions, and tagging standards for **Qixev FieldKit**.

---

## 1. Golden Rules of Agent Git Operations

1. **Explicit Permission Required**: Agents must NEVER execute `git commit`, `git tag`, `git push`, or branch creation without explicit instruction from the user.
2. **Zero Destructive Commands**:
   - `git reset --hard`
   - `git clean -fd`
   - `git push --force`
   The above commands are STRICTLY PROHIBITED unless explicitly commanded by the user with exact commit identifiers.
3. **Strict Git State Classification**:
   When reporting repository status, agents and automated tools must classify the state into one of five explicit tiers:
   - `CLEAN`: Zero staged, tracked unstaged, or untracked changes exist.
   - `DIRTY_TRACKED`: Tracked unstaged modifications exist; zero staged changes; zero untracked files.
   - `DIRTY_UNTRACKED`: Untracked files exist; zero staged changes; zero tracked modifications.
   - `DIRTY_STAGED`: Staged changes exist; zero unstaged modifications; zero untracked files.
   - `DIRTY_MIXED`: Multiple forms of modifications exist simultaneously (e.g. tracked modifications alongside untracked files).
   **Absolute Rule**: The system and agents must NEVER report a repository as "clean" if any tracked, staged, or untracked changes exist. Use `scripts/GitState.ps1` for machine-readable verification.

---

## 2. Commit Message Conventions

FieldKit uses Conventional Commits:

```
<type>: <concise description in lowercase>
```

### Approved Types
- `feat`: User-facing functionality or UI refinement.
- `fix`: Bug fix, exception prevention, or regression repair.
- `refactor`: Structural reorganization without behavioral change.
- `test`: Addition or improvement of unit/instrumentation tests.
- `chore`: Build scripts, documentation, Proguard rules, or `.gitignore` updates.

### Examples
- `feat: add first-run onboarding and product UX`
- `fix: resolve foreign key constraint exception during job cancellation`
- `test: add end-to-end data safety lifecycle test`
- `chore: add automated QA and device diagnostic scripts`

---

## 3. Tagging & Milestones

- Git tags denote stable, verified production milestones (`v0.3-a`, `v0.3-b`, `v0.4-a`, `v0.4-b`, `v0.5-a`, `v0.5-b`).
- An agent must only tag a release after:
  1. All 5 test targets pass (`check`, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, `connectedDebugAndroidTest`).
  2. Physical device verification is confirmed.
  3. The user explicitly requests tag creation.
