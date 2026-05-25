# Performance baselines

`TASK-0043` adds an advisory benchmark smoke lane for generated binding read, write, validate, and
document/open-content workloads. `TASK-0071` extends that lane with schema-to-code generation and
javac observations for the generated document workloads. These measurements are local evidence only.
They are not release guarantees, regression thresholds, or claims about all supported schema shapes.

## Command

```bash
./gradlew benchmarkSmoke --console=plain
```

The root `benchmarkSmoke` task delegates to
`:modules:conformance-tests:benchmarkSmoke`. It is intentionally separate from `qualityGate`.

## Workloads

Generation observations use the same deterministic document schemas as the document runtime
workloads. The `pipeline` field records the end-to-end CoreGenerator path covered by the timing:
schema resolution, XSD syntax parsing, IR build, binding planning, source emission, and source
write. Per-phase internal timings are not exposed by the public generator API yet.

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

## TASK-0071 Local Baseline

Recorded locally on 2026-05-25 EDT with Java 21:

```text
GENERATION_BENCHMARK workload=document profile=XP_XSD10_DOCUMENT schemas=1 pipeline=resolve-parse-ir-bind-emit-write generatedSources=4 sourceBytes=30208 classFiles=6 generationMillis=752.235 javacMillis=2345.422 heapBeforeBytes=0 heapAfterBytes=28406560
GENERATION_BENCHMARK workload=mixed profile=XP_XSD10_DOCUMENT schemas=1 pipeline=resolve-parse-ir-bind-emit-write generatedSources=9 sourceBytes=35875 classFiles=11 generationMillis=254.202 javacMillis=1924.691 heapBeforeBytes=28406560 heapAfterBytes=53572384
BENCHMARK workload=xp-data-10-purchase-read-write-validate iterations=8 inputChars=15601 outputChars=18519 elapsedMillis=623.112 opsPerSecond=12.839 heapBeforeBytes=57766688 heapAfterBytes=61960992
BENCHMARK workload=xp-data-10-multins-read-write-validate iterations=8 inputChars=9380 outputChars=16598 elapsedMillis=549.181 opsPerSecond=14.567 heapBeforeBytes=66155296 heapAfterBytes=19704760
BENCHMARK workload=xp-xsd10-document-wildcard-read-write-validate iterations=8 inputChars=9708 outputChars=12822 elapsedMillis=607.242 opsPerSecond=13.174 heapBeforeBytes=19704760 heapAfterBytes=23899064
BENCHMARK workload=xp-xsd10-document-mixed-read-write-validate iterations=8 inputChars=10007 outputChars=13129 elapsedMillis=644.170 opsPerSecond=12.419 heapBeforeBytes=28093368 heapAfterBytes=32287672
```

The current generated document fixtures do not indicate an urgent runtime throughput regression.
For generation setup, javac is larger than CoreGenerator execution on both measured generated
document workloads. Large-schema risk remains source size and class count growth rather than a
proven runtime read/write/validate bottleneck, so any future threshold should first add broader
generated-source size and phase-timing samples.
