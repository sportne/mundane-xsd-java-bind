# TASK-0019: gradle-plugin-vertical-slice

Status: draft.

Task ID: `TASK-0019`
Gate: Phase 5 first complete public vertical slice; starts only after `TASK-0018` is accepted.
Requirement IDs: `REQ-SCOPE-001`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-NS-001`, `REQ-RES-001`, `REQ-SEC-001`, `REQ-BUILD-001`, `REQ-BUILD-002`, `REQ-QA-001`
ADR IDs: `ADR-0002`, `ADR-0006`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0012`, `ADR-0013`, `ADR-0014`
Specification references: `docs/architecture/module-boundaries.md`, `docs/build/README.md`, `modules/generator-gradle-plugin/README.md`
Target module: `modules/generator-gradle-plugin` and examples
Allowed files: Gradle plugin source/tests, plugin README/docs, example build files and tests needed to exercise generation, and directly related build/traceability docs
Forbidden files: runtime dependency additions, unsupported schema features, CLI behavior changes not required by shared generator API, dependency metadata unless an approved dependency review is added first, and generated product code outside approved examples/golden outputs
Expected behavior: implement a configuration-cache-compatible Gradle task/plugin that generates accepted vertical-slice Java source from explicit schema inputs during task execution, declares inputs/outputs, supports profile and namespace/package mapping configuration, respects resolver policy, and does not resolve schemas during configuration.
Tests to add/update: Gradle TestKit-style plugin tests if available without new dependency review, configuration-cache tests or documented local validation, generated-source compile tests in examples, denied network resolver tests through Gradle configuration, and offline-friendly behavior checks
Documentation to update: plugin README, example READMEs/build files, build docs, offline docs if Gradle generation affects them, and traceability docs
Commands to run: `./gradlew :modules:generator-gradle-plugin:check :examples:purchase-order:check :examples:multi-namespace:check`, `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: example projects can generate and compile source through Gradle; plugin is configuration-cache compatible; schema resolution happens only at execution time; generated output is deterministic
Rollback notes: revert Gradle plugin source/tests/docs and example build/test updates

## Impact Notes

- Coverage: plugin behavior must be tested through task-level execution, not only unit helpers.
- Native Image: no native execution added, but generated example output must be usable by `TASK-0020`.
- Security: network access remains denied by default.
- Documentation: Gradle examples must stay synchronized with actual task names and properties.
