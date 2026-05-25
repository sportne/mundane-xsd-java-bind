# TASK-0071: generation-generated-performance-review

Status: accepted.

Task ID: `TASK-0071`
Priority: P1
Gate: generation and generated-code performance review.
Target areas: benchmark smoke harness, generator-core timing paths, generated reader/writer/validator
workloads, memory observations, performance docs, CI plan, and task handoff.
Allowed files: benchmark harness code, deterministic benchmark fixtures, docs, and tests that assert
functional invariants.
Forbidden files: hard performance guarantees, unstable thresholds in `qualityGate`, dependencies
without ADR, product behavior changes, release metadata, or publication changes.
Expected behavior: measure both schema-to-code generation performance and generated-code runtime
performance. Include schema resolution, syntax parsing, IR build, binding planning, source emission,
javac compile time where practical, generated read/write/validate throughput, output size, source
size, class count, and advisory heap observations. Benchmarks must remain explicit opt-in evidence
unless a later task proves stable thresholds.
Tests to add/update: benchmark functional smoke invariants and deterministic workload generation.
Commands to run: `./gradlew benchmarkSmoke --console=plain`, any new explicit benchmark lane,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: performance is characterized enough to tell whether current behavior is
acceptable, whether large schemas risk unacceptable generation time/source size, and which hotspots
deserve optimization or simplification tasks.
Rollback notes: remove benchmark harness changes and docs.

## Completion notes

`TASK-0071` extends the existing opt-in `benchmarkSmoke` lane instead of adding thresholds or
quality-gate requirements. The lane now emits `GENERATION_BENCHMARK` rows for the generated
document bindings before the existing generated read/write/validate runtime rows. Each generation
row records the end-to-end CoreGenerator pipeline (`resolve-parse-ir-bind-emit-write`), generated
source count, source bytes, javac class count, generator elapsed time, javac elapsed time, and heap
observations.

The local baseline recorded on 2026-05-25 shows:

- `document`: 4 generated sources, 30,208 source bytes, 6 class files, 752.235 ms generation,
  2,345.422 ms javac.
- `mixed`: 9 generated sources, 35,875 source bytes, 11 class files, 254.202 ms generation,
  1,924.691 ms javac.
- Existing runtime workloads stayed functional, with 8-iteration elapsed times from 549.181 ms to
  644.170 ms for the measured run.

The evidence is acceptable for current deterministic fixtures. The main future risk is large-schema
source/class growth and javac setup cost, not a demonstrated generated runtime bottleneck. Per-phase
generator timing remains a future instrumentation candidate because the public generator API exposes
only end-to-end generation results.

## Evidence

- `./gradlew benchmarkSmoke --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
