# TASK-0082: performance-phase-timing

Status: accepted.

Task ID: `TASK-0082`
Priority: P2
Gate: post-1.0.0 follow-up performance characterization.
Requirement IDs: `REQ-PERF-001`, `REQ-QA-002`
ADR IDs: `ADR-0005`, `ADR-0010`, `ADR-0011`, `ADR-0013`
Specification references: `docs/verification/performance-baselines.md`,
`docs/architecture/complexity-review.md`, `docs/infrastructure/ci-plan.md`
Target module: `modules/conformance-tests`
Allowed files: benchmark smoke source/resources, advisory performance docs, traceability docs, and
this task/handoff. Generator API/core changes are allowed only for internal timing capture needed by
the benchmark and must not change public API.
Forbidden files: hard performance thresholds, quality-gate requirements for benchmarks, dependency
metadata, release metadata, support-claim expansion, and unrelated generator refactors.
Expected behavior: `benchmarkSmoke` reports advisory per-phase generator timing and adds a
deterministic large-schema/source-growth characterization sample. Thresholds remain opt-in and
non-claiming.
Tests to add/update: benchmark smoke assertions that phase timing/source-growth output is present
and non-empty; deterministic fixture checks where practical.
Documentation to update: performance baselines, CI plan if command posture is clarified,
traceability, and handoff.
Commands to run: `./gradlew benchmarkSmoke --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: benchmark output includes per-phase timing and large-schema/source-growth
observations; docs state advisory status and no hard guarantees; `qualityGate` remains unchanged.
Rollback notes: revert benchmark and performance-doc updates.

Completion notes:
- Added an internal `generator-core` timing sink used by public `CoreGenerator.generate` only through
  a no-op default path, preserving the public generator API.
- Added a benchmark-only timing probe and `GENERATION_PHASE_BENCHMARK` output for request
  validation, schema resolution, XSD syntax parse, IR build, binding planning, source emission, and
  source write.
- Added a deterministic `large-schema` benchmark sample with 96 optional elements and 32 optional
  attributes, plus `GENERATION_GROWTH_BENCHMARK` source-growth output.
- Updated advisory performance, CI, traceability, and handoff docs without changing `qualityGate` or
  adding hard performance claims.

Evidence:
- `./gradlew benchmarkSmoke --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
