# TASK-0071: generation-generated-performance-review

Status: draft.

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

