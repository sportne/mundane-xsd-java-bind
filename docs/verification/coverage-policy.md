# Coverage policy

| Module type | Aggregate line | Aggregate branch | Per-file line |
|---|---:|---:|---:|
| Runtime core | 90% | 80% | 80% |
| Generator core | 85% | 75% | 75% |
| CLI/plugin | 75% | 65% | 65% |
| Test/build support | 70% | 60% | 60% |

## Enforcement plan

- Phase 0/1: document policy and wire JaCoCo tasks.
- Phase 2: enforce generator-core thresholds for implemented classes.
- Phase 3/4: enforce runtime-core and generated-code test coverage.
- Phase 5: enforce aggregate and per-file thresholds in CI.

## Active enforcement

JaCoCo verification skips modules with no compiled production classes. Once production classes exist, the module-specific bundle line, bundle branch, and per-source-file line thresholds above are enforced by each module's `check` task and the root `qualityGate`.

Generator-core test coverage includes the generated-source verification harness because later generated reader, validation, CLI, and plugin tasks depend on that harness for compile/golden/determinism confidence. Generated fixture smoke sources are compiled and executed by `generatedCodeSmoke`; they are not product classes and do not alter JaCoCo production thresholds.

Native Image smoke tasks do not change JaCoCo thresholds and are not a substitute for JVM coverage
verification. `TASK-0020` keeps the native lane in the separate root `nativeSmoke` aggregate so the
default local and CI `qualityGate` continues to enforce the documented JVM coverage policy without
requiring a local GraalVM installation.

## `0.6.0` hardening coverage posture

`TASK-0041` plans additional conformance, benchmark, Native Image, and release validation lanes
without changing JaCoCo thresholds. Later tasks may add:

- conformance classification and interop tests that count as normal JVM test coverage when they run
  inside existing test tasks;
- `TASK-0043` benchmark smoke checks that prove the benchmark harness runs but do not alter
  coverage thresholds;
- selected Native Image conformance execution that remains separate from JaCoCo;
- publication dry-run checks that validate metadata and artifacts without changing code coverage.

Any threshold increase or new required gate must be accepted by the task that implements it.
