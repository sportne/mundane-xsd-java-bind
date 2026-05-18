# TASK-0040: document-oriented-open-content-readiness

Status: draft.

Task ID: `TASK-0040`
Gate: `0.5.0` Document-Oriented and Open Content readiness; starts only after `TASK-0039` is accepted.
Requirement IDs: designed `REQ-SCHEMA-013`, designed `REQ-BIND-004`, designed `REQ-XML-R-002`, designed `REQ-XML-W-002`, designed `REQ-VAL-008`, generation, interop, Native Image, QA, and documentation IDs
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance, verification evidence, examples, release notes if present
Allowed files: requirements docs, conformance docs, verification docs, README/module/example docs, agent handoff/task cards, release docs
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.5.0` `XP-XSD10-DOCUMENT` document-oriented/open-content vertical slice with conformance status, interop evidence, Native Image evidence, limitations, and next-slice draft readiness.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, round-trip, and Native Image evidence only
Documentation to update: all user-facing support/conformance docs affected by `0.5.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented native command, `git diff --check`
Acceptance criteria: open-content support claims match evidence; formal canonicalization claims are avoided unless proven; interop evidence is recorded
Rollback notes: revert readiness-review docs and release metadata from this task

## Impact Notes

- Interop: readiness requires recorded validation and serialization evidence or explicit limitation notes.
- Native Image: representative open-content fixtures should run in selected lanes.
- Security: unknown/mixed content limits remain verified.
- Documentation: distinguish supported document-oriented cases from full XML ecosystem support.

## Readiness Checks

- Confirm support claims cover only accepted `TASK-0037`, `TASK-0038`, and `TASK-0039`
  `XP-XSD10-DOCUMENT` behavior.
- Confirm conformance rows for wildcard/open content, mixed content, and document serialization
  policy match automated and documented interop evidence.
- Confirm unsupported document constructs still produce explicit diagnostics, including
  `xs:anyAttribute`, `processContents="lax"` or `"strict"`, wildcard choices, unsupported
  namespace constraints, mixed choices, comments/PI retention, entity-reference semantics,
  DOM-backed binding, identity constraints, full datatype semantics, full derivation semantics, and
  XSD 1.1.
- Confirm release docs still do not claim full XSD 1.0 conformance, XML Canonicalization,
  cryptographic canonical XML compatibility, artifact publication, or a release tag unless a
  separate release task authorizes it.
