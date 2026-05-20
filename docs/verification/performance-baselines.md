# Performance baselines

`TASK-0043` adds an advisory benchmark smoke lane for generated binding read, write, validate, and
document/open-content workloads. These measurements are local evidence only. They are not release
guarantees, regression thresholds, or claims about all supported schema shapes.

## Command

```bash
./gradlew benchmarkSmoke --console=plain
```

The root `benchmarkSmoke` task delegates to
`:modules:conformance-tests:benchmarkSmoke`. It is intentionally separate from `qualityGate`.

## Workloads

| Workload | Fixture shape | Iterations | Input chars | Output chars |
|---|---|---:|---:|---:|
| `xp-data-10-purchase-read-write-validate` | 240 generated purchase-order lines | 8 | 15601 | 18519 |
| `xp-data-10-multins-read-write-validate` | 240 generated cross-namespace lines | 8 | 9380 | 16598 |
| `xp-xsd10-document-wildcard-read-write-validate` | 120 retained wildcard fragments | 8 | 9708 | 12822 |
| `xp-xsd10-document-mixed-read-write-validate` | 120 mixed text/wildcard branches | 8 | 10007 | 13129 |

## TASK-0043 Local Baseline

Recorded locally on 2026-05-19 EDT with Java 21:

```text
BENCHMARK workload=xp-data-10-purchase-read-write-validate iterations=8 inputChars=15601 outputChars=18519 elapsedMillis=533.823 opsPerSecond=14.986 heapBeforeBytes=48650088 heapAfterBytes=57038696
BENCHMARK workload=xp-data-10-multins-read-write-validate iterations=8 inputChars=9380 outputChars=16598 elapsedMillis=518.857 opsPerSecond=15.419 heapBeforeBytes=57038696 heapAfterBytes=61233000
BENCHMARK workload=xp-xsd10-document-wildcard-read-write-validate iterations=8 inputChars=9708 outputChars=12822 elapsedMillis=442.018 opsPerSecond=18.099 heapBeforeBytes=61233000 heapAfterBytes=69621608
BENCHMARK workload=xp-xsd10-document-mixed-read-write-validate iterations=8 inputChars=10007 outputChars=13129 elapsedMillis=511.073 opsPerSecond=15.653 heapBeforeBytes=69621608 heapAfterBytes=73815912
```

The benchmark harness fails only functional smoke invariants: successful read, write, validation,
non-empty output, and generated-binding round trip. Timing and heap values are advisory observations.
