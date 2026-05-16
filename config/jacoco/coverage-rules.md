# JaCoCo coverage rules

Active thresholds:

| Module type | Aggregate line | Aggregate branch | Per-file line |
|---|---:|---:|---:|
| Runtime core | 90% | 80% | 80% |
| Generator core | 85% | 75% | 75% |
| CLI/plugin | 75% | 65% | 65% |
| Build/test support | 70% | 60% | 60% |

Modules with no compiled production classes skip JaCoCo verification. Implemented modules enforce module-specific bundle line, bundle branch, and per-source-file line thresholds through `check` and the root `qualityGate`.
