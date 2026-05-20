# TASK-0043: performance-memory-streaming-benchmarks

Status: accepted.

Task ID: `TASK-0043`
Gate: `0.6.0` Hardening and Release Maturity; starts only after `TASK-0042` is accepted.
Requirement IDs: future `REQ-PERF-*`, accepted generation, reader, writer, validation, QA, and docs IDs
ADR IDs: `ADR-0005`, `ADR-0010`, `ADR-0011`, `ADR-0013`
Specification references: `docs/verification/verification-plan.md`, `docs/architecture/runtime-architecture.md`, `docs/infrastructure/ci-plan.md`
Target modules: benchmark or test support areas approved by `TASK-0041`, examples, conformance tests
Allowed files: benchmark fixtures, performance test harness, docs, CI wiring only if accepted by planning
Forbidden files: unplanned runtime redesign, dependency metadata unless approved, quality-gate weakening, unsupported feature implementation
Expected behavior: add measurable performance, memory, and streaming benchmark fixtures for representative generated bindings and document baseline targets without converting benchmarks into unstable default gates.
Tests added/updated: benchmark smoke source set, explicit benchmark smoke tasks, deterministic generated fixture workloads, memory/streaming observations, documentation evidence
Documentation to update: verification plan, performance requirements, CI plan, release plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, documented benchmark command, `git diff --check`
Acceptance criteria: benchmarks are repeatable from local fixtures; baseline results and any advisory thresholds are documented; default `qualityGate` remains stable; no unsupported performance or streaming guarantee is made
Rollback notes: revert benchmarks, fixtures, CI/docs changes from this task

## Accepted Planning Scope

- Prefer a lightweight Gradle benchmark or performance-smoke lane using existing Java tooling before
  adding dependencies; any dependency requires explicit review in this task.
- Cover generated read, write, validate, and mixed/open-content serialization workloads with
  bounded local fixtures.
- Record baseline wall-clock, allocation/memory, or input-size observations as advisory evidence,
  not hard release guarantees unless the task proves stable thresholds.
- Keep large fixtures local, deterministic, and network-free.

## Impact Notes

- Interop: reuse interop fixtures where they provide realistic workload comparisons.
- Native Image: identify benchmark cases that should also run in native smoke/conformance lanes.
- Security: large fixtures must not require network access.
- Documentation: distinguish benchmark baselines from hard performance guarantees.

## Implementation Evidence

- Added `:modules:conformance-tests:benchmarkSmoke` as an explicit `JavaExec` benchmark smoke lane
  using a `benchmarkSmoke` source set and existing project dependencies only.
- Added root `benchmarkSmoke` aggregate task; it is intentionally separate from `check`,
  `checkAll`, and `qualityGate`.
- Added deterministic workloads for generated purchase-order, multi-namespace, wildcard
  open-content, and mixed-content bindings. Document-profile generated bindings are generated and
  compiled during benchmark setup, outside measured loops.
- Added machine-readable output with workload name, iterations, input/output characters, elapsed
  milliseconds, operations per second, and heap before/after observations.
- Added `docs/verification/performance-baselines.md` with the local advisory baseline and explicit
  non-guarantee language.

## Verification Evidence

- `./gradlew :modules:conformance-tests:benchmarkSmoke --console=plain` passed.
- `./gradlew benchmarkSmoke --console=plain` passed and recorded the baseline in
  `docs/verification/performance-baselines.md`.
- `./gradlew :modules:conformance-tests:check --console=plain` passed.
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
