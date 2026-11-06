---
name: qixev-product-audit
description: Non-invasive product audit protocol for Qixev FieldKit. Evaluates UX, user workflows, data safety, accessibility, privacy, performance, monetization readiness, and Android compatibility without modifying code. Produces prioritized P0?P3 findings.
---

# Qixev Product & UX Audit Protocol

This skill guides agents through performing structured, non-invasive evaluations of **Qixev FieldKit**. It reviews the codebase, assets, and runtime experience without altering application behavior or modifying code.

---

## 1. Audit Dimensions

When executing a product audit, evaluate the following 11 dimensions:

1. **User Workflow**:
   - Repair intake -> Customer & device info -> Status progression -> Parts consumed -> Cost calculation -> Delivery.
   - Inventory intake -> SKU & category assignment -> Stock level alerts.
2. **UX & Visual Polish**:
   - Material You alignment, typography hierarchy, empty states, search and filter responsiveness.
3. **Reliability & Concurrency**:
   - Room transaction safety, foreign key cascades, negative stock guards, status state machine locks.
4. **Accessibility (a11y)**:
   - Touch targets ($\ge 48\times 48\text{ dp}$), content descriptions for screen readers, high-contrast text scaling, keyboard actions.
5. **Performance**:
   - Compose recomposition stability, lazy list keying, database query indexing, memory pressure on 2GB/3GB devices.
6. **Privacy**:
   - Absolute local-first guarantee, zero network permissions, zero telemetry or ad tracking SDKs.
7. **Security**:
   - Secret exclusion, scoped storage usage, lack of exported components with intent filters except `MainActivity`.
8. **Data Safety**:
   - Versioned JSON backup determinism, atomic database restores with automatic rollback, CSV export integrity.
9. **Monetization Readiness**:
   - Clean architectural boundaries for potential future offline feature licensing or pro exports without core refactoring.
10. **Distribution Readiness**:
    - APK size, R8 minification, vector icon assets, proper launcher mipmap resolutions (`anydpi-v26`), app labels.
11. **Android Compatibility**:
    - `minSdk 26` (Android 8.0) to `targetSdk 37`, edge-to-edge support, window soft input mode.

---

## 2. Priority Classification Framework

All audit findings MUST be categorized into one of four priority tiers:

- **P0 ? Ship Blocker**:
  - Unhandled crash, data loss, stock corruption, negative inventory, missing migration causing crash, exposed secret, broken release signing.
- **P1 ? Important Before Monetization**:
  - Missing validation causing UI glitch, broken edge-to-edge layout, inconsistent terminology, missing empty state CTA, inaccessible touch target on core flow.
- **P2 ? Worthwhile Improvement**:
  - Performance optimization (e.g. memoization of formatters), additional filter criteria, UX micro-copy polish, layout animation refinement.
- **P3 ? Later / Backlog**:
  - Future cosmetic enhancements, optional export formats, extended keyboard shortcuts.

---

## 3. Audit Execution Steps

1. Inspect active milestone and Git commit: `git log -n 1 --oneline`.
2. Review UI screens, dialogs, and navigation in `app/src/main/java/com/qixev/qfieldkit/ui/`.
3. Review data models and database migrations in `app/src/main/java/com/qixev/qfieldkit/data/`.
4. Review manifest permissions and configuration in `app/src/main/AndroidManifest.xml`.
5. Run static analysis: `.\gradlew check`.
6. Inspect generated release APK size and R8 output: `.\gradlew assembleRelease`.
7. Summarize findings in a clean Markdown report with explicit P0, P1, P2, P3 sections.
