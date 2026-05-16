# TASK-0018: generator-api-and-cli-vertical-slice

Status: draft.

Task ID: `TASK-0018`
Gate: Phase 5 first complete public vertical slice; starts only after `TASK-0017` is accepted.
Requirement IDs: `REQ-SCOPE-001`, `REQ-SCOPE-002`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-NS-001`, `REQ-RES-001`, `REQ-SEC-001`, `REQ-BUILD-002`, `REQ-QA-001`
ADR IDs: `ADR-0002`, `ADR-0006`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/module-boundaries.md`, `docs/architecture/compiler-pipeline.md`, `modules/generator-api/README.md`, `modules/generator-cli/README.md`
Target modules: `modules/generator-api`, `modules/generator-cli`, `modules/generator-core`
Allowed files: public generator configuration/profile types, CLI entry point and tests, generator-core adapter code needed to expose the accepted pipeline through the public interface, CLI docs/README updates, and directly related traceability docs
Forbidden files: Gradle plugin implementation, runtime dependency additions, unsupported schema features, generated runtime reflection/annotation behavior, code-to-schema features, dependency metadata unless an approved dependency review is added first
Expected behavior: expose a minimal public generator interface and CLI command that accepts schema inputs, explicit output directory, profile selection, namespace/package mapping configuration, and resolver/catalog options; it must write deterministic generated Java source for the accepted vertical slice and reject code-to-schema or out-of-profile requests.
Tests to add/update: generator-api contract tests, CLI invocation tests, deterministic output directory tests, denied network CLI tests, invalid option diagnostics, no code-to-schema surface tests, generated source compile tests, and docs command examples where practical
Documentation to update: generator-api README, generator-cli README, README command examples, traceability, and build docs if public tasks/commands are documented
Commands to run: `./gradlew :modules:generator-api:check :modules:generator-cli:check :modules:generator-core:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: users can run the CLI against approved fixtures and receive deterministic generated source; API does not expose parser internals; generator dependencies do not leak into runtime or generated-code paths
Rollback notes: revert generator-api/CLI/source/tests/docs changes and directly related pipeline adapter updates

## Impact Notes

- Coverage: public interface and CLI behavior require black-box tests.
- Native Image: CLI must remain compatible with later native smoke testing.
- Security: resolver/network behavior must be configurable only through explicit policy.
- Documentation: CLI examples must be executable and scope-limited.
