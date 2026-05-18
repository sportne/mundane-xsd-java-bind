# TASK-0035: xsd10-semantic-expansion-readiness

Status: draft.

Task ID: `TASK-0035`
Gate: `0.4.0` XSD 1.0 Semantic Expansion readiness; starts only after `TASK-0034` is accepted.
Requirement IDs: accepted `0.4.0` schema, binding, model, validation, generation, interop, Native Image, QA, and documentation IDs
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance, verification evidence, examples, release notes if present
Allowed files: requirements docs, conformance docs, verification docs, README/module/example docs, agent handoff/task cards, release docs
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.4.0` semantic expansion vertical slice for `XP-XSD10-SEMANTIC` with conformance status, interop evidence, Native Image evidence, limitations, and next-slice draft readiness.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, round-trip, and Native Image evidence only
Documentation to update: all user-facing support/conformance docs affected by `0.4.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented native command, `git diff --check`
Acceptance criteria: semantic support claims match test evidence; interop evidence is recorded; document-oriented features remain future
Rollback notes: revert readiness-review docs and release metadata from this task

## Impact Notes

- Interop: readiness requires recorded evidence or explicit limitation notes.
- Native Image: semantic fixtures should run in selected lanes.
- Security: semantic diagnostics remain safe and deterministic.
- Documentation: no wildcard/mixed-content claims.

## Readiness Checks

- Confirm support claims cover only accepted `TASK-0032`, `TASK-0033`, and `TASK-0034`
  `XP-XSD10-SEMANTIC` behavior.
- Confirm conformance rows for nillable/default/fixed semantics, direct substitution groups, and
  semantic validation match automated and documented interop evidence.
- Confirm unsupported semantic, substitution, identity-constraint, wildcard, mixed-content,
  full-datatype, full-derivation, and XSD 1.1 shapes still produce explicit diagnostics.
- Confirm release docs still do not claim full XSD 1.0 conformance, artifact publication, or a
  release tag unless a separate release task authorizes it.
