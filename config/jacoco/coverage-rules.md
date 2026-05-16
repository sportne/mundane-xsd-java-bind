# JaCoCo coverage rules

Initial staged thresholds:

| Module type | Aggregate line | Aggregate branch | Per-file line |
|---|---:|---:|---:|
| Runtime core | 90% | 80% | 80% |
| Generator core | 85% | 75% | 75% |
| CLI/plugin | 75% | 65% | 65% |
| Build/test support | 70% | 60% | 60% |

The scaffold starts with a low temporary bundle threshold in build logic because there is no implementation. The first implementation PR must replace the staged threshold with module-specific rules matching this policy.
