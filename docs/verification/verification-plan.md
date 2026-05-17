# Verification and validation plan

## Test layers

| Layer | Purpose |
|---|---|
| Unit tests | Parser/model/binding/code-emitter/runtime primitives. |
| Golden source tests | Generated source must match approved output exactly. |
| Compile tests | Generated source compiles under Java 21 and Java 25 lanes. |
| Round-trip tests | Object → XML → object and XML → object → XML. |
| Negative tests | Invalid XML/schema/profile inputs produce deterministic diagnostics. |
| Differential tests | Compare selected behavior with JDK XML validation/tooling where useful. |
| Conformance harness | Run selected W3C XML/XSD tests by profile. |
| Architecture tests | Enforce module boundaries and forbidden runtime behavior. |
| Static analysis | Checkstyle, Spotless, SpotBugs, Error Prone. |
| Coverage gates | JaCoCo aggregate and per-file thresholds. |
| Native tests | Compile and execute runtime primitives and generated sample bindings as native images when each surface becomes executable. |
| Security tests | XXE, entity expansion, resolver denial, excessive nesting. |
| Documentation tests | Verify examples, commands, requirement and ADR trace links. |

## Generated-source harness

`TASK-0013` establishes the canonical generated-source verification harness for generated model/writer source and later reader/validation source.

- Golden fixtures live under generator-core test resources with generated relative paths and a `.java.golden` suffix so formatters do not rewrite approved output.
- Generated source tests compare byte-for-byte against approved golden fixtures, reject duplicate relative output paths, verify deterministic repeated emission, compile with Java 21 `-Xlint:all -Werror`, and execute model, reader, writer, and validator behavior through loaded generated classes.
- `:modules:generator-core:generatedCodeSmoke` compiles approved generated fixtures plus a small smoke main and is part of `:modules:generator-core:check`; after `TASK-0016` it also verifies generated validation results.
- `:modules:generator-core:generatedCodeNativeSmoke` builds and runs the same approved generated fixture path as a Native Image smoke executable when GraalVM native-image is available.
- `TASK-0017` adds executable purchase-order and multi-namespace example fixtures that exercise
  generated model, reader, writer, and validator sources through JDK XML adapters.
- `TASK-0020` adds the root `nativeSmoke` aggregate for runtime-core, runtime-jdkxml,
  generator-core generated-code smoke, and both representative example native tests. The normal
  `qualityGate` remains JVM-focused; the native CI workflow runs
  `./gradlew validateDesignControlPack nativeSmoke --console=plain`.

## Phase-one verification minimum

- XSD fixture → binding IR golden test.
- XSD fixture → generated source golden test.
- Generated source compile test.
- XML input → object test.
- Object → XML output test.
- Object/XML → validation result test.
- Round-trip test.
- Negative XML diagnostic test.
- Basic Native Image smoke test.

## `0.2.0` planned verification minimum

`TASK-0023` adds executable evidence for `XP-DATA-10-CHOICE`. `TASK-0024` must add the executable
tests for `XP-VALIDATION-10-BASIC`.

- Choice support: `T-CHOICE-*` frontend, IR, binding, model, writer, reader, validator,
  CoreGenerator, CLI, Gradle plugin, generated-source compilation, default-profile rejection, and
  repeated-choice diagnostic evidence.
- Facet support: `T-FACET-*` frontend, IR, binding, generated source, validator, diagnostic,
  round-trip, conformance, interop, and representative Native Image smoke evidence.
- Interop: at least one positive and one negative fixture for each accepted choice/facet group should
  be compared against JDK XML Schema validation where practical; any gap must be documented before
  readiness acceptance.
- Native Image: new `0.2.0` fixtures join representative smoke coverage in their implementation
  tasks. Native Image conformance breadth remains a later `TASK-0044` concern.
- Release posture: readiness evidence must not create a `0.1.0` release tag or claim publication
  readiness without a separate release-engineering task.
