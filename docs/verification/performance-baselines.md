# Performance baselines

`TASK-0043` adds an advisory benchmark smoke lane for generated binding read, write, validate, and
document/open-content workloads. `TASK-0071` extends that lane with schema-to-code generation and
javac observations for the generated document workloads. `TASK-0082` adds benchmark-only generator
phase timings and a deterministic large-schema source-growth sample. These measurements are local
evidence only. They are not release guarantees, regression thresholds, or claims about all supported
schema shapes.

## Command

```bash
./gradlew benchmarkSmoke --console=plain
```

The root `benchmarkSmoke` task delegates to
`:modules:conformance-tests:benchmarkSmoke`. It is intentionally separate from `qualityGate`.

## Workloads

Generation observations use the same deterministic document schemas as the document runtime
workloads plus one generated large-schema fixture. The `pipeline` field records the end-to-end
CoreGenerator path covered by the timing. `GENERATION_PHASE_BENCHMARK` rows split that timing into
request validation, schema resolution, XSD syntax parsing, IR build, binding planning, source
emission, and source write. The phase hook is internal to `generator-core` and is exercised through
benchmark-only source; it is not a public generator API.

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

## TASK-0082 Local Baseline

Recorded locally on 2026-05-25 EDT with Java 21:

```text
GENERATION_BENCHMARK workload=document profile=XP_XSD10_DOCUMENT schemas=1 pipeline=resolve-parse-ir-bind-emit-write generatedSources=4 sourceBytes=30208 classFiles=6 generationMillis=583.758 javacMillis=1103.927 heapBeforeBytes=0 heapAfterBytes=27465624
GENERATION_PHASE_BENCHMARK workload=document phase=request-validation elapsedMillis=0.013 elapsedNanos=12623
GENERATION_PHASE_BENCHMARK workload=document phase=schema-resolution elapsedMillis=51.510 elapsedNanos=51510187
GENERATION_PHASE_BENCHMARK workload=document phase=xsd-syntax-parse elapsedMillis=28.619 elapsedNanos=28618711
GENERATION_PHASE_BENCHMARK workload=document phase=ir-build elapsedMillis=120.088 elapsedNanos=120088365
GENERATION_PHASE_BENCHMARK workload=document phase=binding-plan elapsedMillis=146.374 elapsedNanos=146373739
GENERATION_PHASE_BENCHMARK workload=document phase=source-emission elapsedMillis=114.234 elapsedNanos=114233767
GENERATION_PHASE_BENCHMARK workload=document phase=output-write elapsedMillis=66.553 elapsedNanos=66552926
GENERATION_BENCHMARK workload=mixed profile=XP_XSD10_DOCUMENT schemas=1 pipeline=resolve-parse-ir-bind-emit-write generatedSources=9 sourceBytes=35875 classFiles=11 generationMillis=129.636 javacMillis=948.068 heapBeforeBytes=27465624 heapAfterBytes=61020056
GENERATION_PHASE_BENCHMARK workload=mixed phase=request-validation elapsedMillis=0.004 elapsedNanos=3936
GENERATION_PHASE_BENCHMARK workload=mixed phase=schema-resolution elapsedMillis=11.079 elapsedNanos=11079157
GENERATION_PHASE_BENCHMARK workload=mixed phase=xsd-syntax-parse elapsedMillis=10.906 elapsedNanos=10905872
GENERATION_PHASE_BENCHMARK workload=mixed phase=ir-build elapsedMillis=0.398 elapsedNanos=397839
GENERATION_PHASE_BENCHMARK workload=mixed phase=binding-plan elapsedMillis=8.658 elapsedNanos=8658209
GENERATION_PHASE_BENCHMARK workload=mixed phase=source-emission elapsedMillis=4.562 elapsedNanos=4562117
GENERATION_PHASE_BENCHMARK workload=mixed phase=output-write elapsedMillis=93.707 elapsedNanos=93706555
GENERATION_BENCHMARK workload=large-schema profile=XP_XSD10_FULL schemas=1 pipeline=resolve-parse-ir-bind-emit-write generatedSources=4 sourceBytes=153020 classFiles=4 generationMillis=98.709 javacMillis=1229.290 heapBeforeBytes=61020056 heapAfterBytes=82839520
GENERATION_PHASE_BENCHMARK workload=large-schema phase=request-validation elapsedMillis=0.006 elapsedNanos=5524
GENERATION_PHASE_BENCHMARK workload=large-schema phase=schema-resolution elapsedMillis=20.523 elapsedNanos=20523156
GENERATION_PHASE_BENCHMARK workload=large-schema phase=xsd-syntax-parse elapsedMillis=10.868 elapsedNanos=10868294
GENERATION_PHASE_BENCHMARK workload=large-schema phase=ir-build elapsedMillis=4.633 elapsedNanos=4632689
GENERATION_PHASE_BENCHMARK workload=large-schema phase=binding-plan elapsedMillis=3.576 elapsedNanos=3575822
GENERATION_PHASE_BENCHMARK workload=large-schema phase=source-emission elapsedMillis=9.172 elapsedNanos=9172452
GENERATION_PHASE_BENCHMARK workload=large-schema phase=output-write elapsedMillis=49.812 elapsedNanos=49811511
GENERATION_GROWTH_BENCHMARK workload=large-schema elements=96 attributes=32 schemaMembers=128 generatedSources=4 sourceBytes=153020 classFiles=4 sourceBytesPerMember=1195.469
BENCHMARK workload=xp-data-10-purchase-read-write-validate iterations=8 inputChars=15601 outputChars=18519 elapsedMillis=287.361 opsPerSecond=27.840 heapBeforeBytes=82839520 heapAfterBytes=91228128
BENCHMARK workload=xp-data-10-multins-read-write-validate iterations=8 inputChars=9380 outputChars=16598 elapsedMillis=410.694 opsPerSecond=19.479 heapBeforeBytes=91228128 heapAfterBytes=95422432
BENCHMARK workload=xp-xsd10-document-wildcard-read-write-validate iterations=8 inputChars=9708 outputChars=12822 elapsedMillis=400.709 opsPerSecond=19.965 heapBeforeBytes=95422432 heapAfterBytes=99616736
BENCHMARK workload=xp-xsd10-document-mixed-read-write-validate iterations=8 inputChars=10007 outputChars=13129 elapsedMillis=408.985 opsPerSecond=19.561 heapBeforeBytes=103811040 heapAfterBytes=108005344
```

The large-schema fixture is synthetic and deterministic: one qualified root, 96 optional scalar
elements, and 32 optional scalar attributes. It characterizes generated source growth and compile
setup pressure only; it is not a claim that all large schemas have the same shape or cost.
