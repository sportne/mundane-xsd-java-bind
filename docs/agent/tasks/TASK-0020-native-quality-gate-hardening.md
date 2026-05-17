# TASK-0020: native-quality-gate-hardening

Status: accepted.

Task ID: `TASK-0020`
Gate: Phase 5 first complete public vertical slice; starts only after `TASK-0019` is accepted.
Requirement IDs: `REQ-NI-001`, `REQ-QA-001`, `REQ-BUILD-002`, `REQ-BUILD-003`, `REQ-GEN-002`, `REQ-RT-001`, `REQ-RT-002`, `REQ-SEC-001`
ADR IDs: `ADR-0003`, `ADR-0004`, `ADR-0010`, `ADR-0011`, `ADR-0012`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/native-image-architecture.md`, `docs/verification/native-image-test-plan.md`, `docs/verification/coverage-policy.md`, `docs/infrastructure/ci-plan.md`
Target areas: build logic, CI workflows, examples, conformance tests, and verification docs
Allowed files: native-image workflow/config, build-logic native/coverage conventions, example native smoke tests, CI docs, coverage docs, conformance/native test docs, and directly related traceability docs
Forbidden files: unsupported XML/XSD behavior, generated-code runtime reflection enablement, broad quality-gate weakening, dependency metadata unless an approved dependency review is added first
Expected behavior: harden and broaden the Native Image and quality gates introduced by `TASK-0010`, `TASK-0013`, and `TASK-0017` for the completed vertical slice: representative generated sample bindings compile and execute as native images, coverage thresholds remain enforced for implemented modules according to policy, CI exercises Java 21 and Java 25 lanes, and staged exceptions remain explicit.
Tests to add/update: expanded native smoke tests for purchase-order and multi-namespace generated bindings, CI/local native task validation, coverage verification for implemented modules, ArchUnit/static-analysis enforcement for forbidden runtime behavior, and denied-resource security tests in generated paths
Documentation to update: native-image test plan, coverage policy, CI plan, build docs, conformance matrix, traceability, and agent handoff if gates change
Commands to run: `./gradlew validateDesignControlPack qualityGate`, native-image smoke command documented by this task, `git diff --check`
Acceptance criteria: native smoke tests run for representative generated bindings; earlier native gates remain active or have documented blockers; quality gates fail meaningfully on implemented code; no gate is weakened without ADR; all staged exceptions are documented
Rollback notes: revert native/coverage/build/CI/test/docs changes from this task

## Impact Notes

- Coverage: this task preserves the active coverage policy for modules with implemented production classes.
- Native Image: this task hardens the existing native smoke/conformance lane; it must not be the first task to exercise native compatibility.
- Security: native and JVM paths must preserve denied network/resource behavior.
- Documentation: any change to gates must be reflected in contributor-facing build docs.

## Completion Notes

- Added the root `nativeSmoke` aggregate for runtime-core, runtime-jdkxml, generator-core
  generated-code, purchase-order, and multi-namespace Native Image smoke tasks.
- Updated the native-image CI workflow to run
  `./gradlew validateDesignControlPack nativeSmoke --console=plain` on the existing GraalVM
  Java 21 and Java 25 matrix while keeping `qualityGate` JVM-focused.
- Added generated-validator example coverage for secure-adapter entity/resource denial.
- Updated native, verification, coverage, CI, build, conformance, traceability, and handoff docs.
- Verification: `./gradlew validateDesignControlPack qualityGate`,
  `JAVA_HOME=/home/jack/.sdkman/candidates/java/21.0.2-graalce PATH=/home/jack/.sdkman/candidates/java/21.0.2-graalce/bin:$PATH ./gradlew nativeSmoke --console=plain`,
  and `git diff --check` passed before acceptance was recorded.
