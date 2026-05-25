# TASK-0079: conformance-schemafactory-hardening

Status: draft.

Task ID: `TASK-0079`
Priority: P0
Gate: post-1.0.0 follow-up security.
Requirement IDs: `REQ-SEC-001`, `REQ-CONF-001`, `REQ-QA-002`
ADR IDs: `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/verification/xml-security-posture-review.md`,
`docs/verification/security-test-plan.md`, `docs/architecture/security-architecture.md`
Target module: `modules/conformance-tests`
Allowed files: conformance test helpers/tests/resources, W3C intake helpers only when reused as a
shared hardened helper, security/conformance verification docs, traceability docs, and this
task/handoff.
Forbidden files: product generator/runtime behavior unless a confirmed production parser path is
found, dependency metadata, release metadata, support-claim expansion, and quality-gate weakening.
Expected behavior: conformance test JDK XML Schema validation helpers deny external DTD and schema
access consistently. Test-only schema oracles must not use plain `SchemaFactory.newInstance(...)`
without hardened `ACCESS_EXTERNAL_DTD` and `ACCESS_EXTERNAL_SCHEMA` settings.
Tests to add/update: focused regression proving conformance schema factories reject external DTD or
external schema access, plus affected conformance tests updated to use the hardened helper.
Documentation to update: XML security posture review, security test plan, conformance matrix if
evidence changes, traceability, and handoff.
Commands to run: `./gradlew :modules:conformance-tests:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, `git diff --check`, and the
SDKMAN GraalVM native lane only if native/security coverage is changed.
Acceptance criteria: no remaining conformance test helper creates an unhardened JDK XML Schema
factory; external resource denial is covered by stable tests; no product support or release claim is
broadened.
Rollback notes: revert conformance helper/test/doc updates.

