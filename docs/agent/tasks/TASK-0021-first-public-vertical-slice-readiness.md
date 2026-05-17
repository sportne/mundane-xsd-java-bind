# TASK-0021: first-public-vertical-slice-readiness

Status: accepted.

Task ID: `TASK-0021`
Gate: Phase 5 first complete public vertical slice release-readiness review; starts only after `TASK-0020` is accepted.
Requirement IDs: `REQ-SCOPE-001`, `REQ-SCOPE-002`, `REQ-SCHEMA-*`, `REQ-RES-001`, `REQ-SEC-001`, `REQ-NS-001`, `REQ-GEN-*`, `REQ-MODEL-001`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-VAL-*`, `REQ-RT-*`, `REQ-NI-001`, `REQ-QA-001`, `REQ-DOC-*`, `REQ-REL-*`, `REQ-AGENT-001`
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `README.md`, `docs/charter.md`, `docs/roadmap.md`, `docs/requirements/phase-1-requirements.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, verification records, examples, release/conformance metadata, and agent handoff
Allowed files: README, CONTRIBUTING, module/example READMEs, requirements docs, conformance docs, verification docs, release docs, agent handoff/task cards, changelog/release notes if added by approved release policy
Forbidden files: new product behavior, unsupported schema expansion, new dependencies, quality-gate weakening, generated output changes unless they correct documented readiness evidence
Expected behavior: perform the first public vertical slice readiness review, align docs with implemented behavior, mark implemented/verified requirement statuses, record conformance status honestly, confirm examples and commands work, and define the next post-slice roadmap task without silently broadening scope.
Tests to add/update: documentation command checks where available, final quality gate, native smoke tests, round-trip/conformance tests, CLI and Gradle example verification, dependency/ArchUnit checks, and `git diff --check`
Documentation to update: all contributor/user-facing docs that describe supported behavior, setup, commands, profiles, conformance status, and limitations
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented native-image smoke command, documented CLI and Gradle example commands, `git diff --check`
Acceptance criteria: first success milestone in `docs/charter.md` is demonstrably met; docs do not overclaim support; requirement statuses and conformance rows match test evidence; next roadmap step is documented as a separate task
Rollback notes: revert readiness-review docs and any release metadata added by this task

## Impact Notes

- Coverage: final verification must account for implemented module thresholds and exceptions.
- Native Image: native smoke results are required readiness evidence.
- Security: security test evidence must cover resolver and XML reader paths.
- Documentation: this task is primarily documentation and verification alignment, not new implementation.

## Completion Notes

- Verified `TASK-0021` as the correct next gate after accepted `TASK-0020`; at `TASK-0021`
  acceptance time, `TASK-0022+` remained draft.
- Normalized `TASK-0006` status from `approved` to accepted to match handoff and implementation evidence.
- Updated public and contributor docs to distinguish the implemented `XP-DATA-10` slice from future
  `xs:choice`, simple-type facets, derivation, open content, mixed content, identity constraints,
  and XSD 1.1 work.
- Updated requirement, traceability, conformance, security, release, module, and example docs to
  reflect verified first-slice evidence without claiming publication readiness.
- Verification: `./gradlew clean validateDesignControlPack qualityGate`,
  `./gradlew :modules:generator-cli:run --args="generate --schema ${PWD}/examples/purchase-order/src/main/resources/schema/purchase-order.xsd --output ${PWD}/build/generated/mxjb-readiness-cli"`,
  `./gradlew :examples:purchase-order:check :examples:multi-namespace:check`,
  `JAVA_HOME=/home/jack/.sdkman/candidates/java/21.0.2-graalce PATH=/home/jack/.sdkman/candidates/java/21.0.2-graalce/bin:$PATH ./gradlew nativeSmoke --console=plain`,
  and `git diff --check` passed before acceptance was recorded.
