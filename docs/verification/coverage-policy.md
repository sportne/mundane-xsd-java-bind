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
