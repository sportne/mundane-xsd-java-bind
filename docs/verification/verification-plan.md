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

## `0.2.0` readiness verification evidence

`TASK-0023` adds executable JVM implementation evidence for `XP-DATA-10-CHOICE`, including
conformance/interop fixtures compared against JDK XML Schema validation and representative
generated-code smoke coverage for the Native Image lane. `TASK-0024` adds the corresponding
executable evidence for `XP-VALIDATION-10-BASIC`.

- Choice support: `T-CHOICE-*` frontend, IR, binding, model, writer, reader, validator,
  CoreGenerator, CLI, Gradle plugin, generated-source compilation, default-profile rejection, and
  repeated-choice diagnostic evidence.
- Choice conformance/interop: `T-CONF-XP-DATA-10-CHOICE-*`, `T-INTEROP-CHOICE-*`, and
  representative generated-code/native-smoke choice evidence are part of `TASK-0023` acceptance.
- Facet support: `T-FACET-*` frontend, IR, binding, generated source, validator, diagnostic,
  round-trip, conformance, interop, and representative Native Image smoke evidence.
- Facet conformance/interop: `T-CONF-XP-VALIDATION-10-BASIC-*`, `T-INTEROP-FACET-*`, and
  representative generated-code/native-smoke facet evidence are part of `TASK-0024` acceptance.
- Interop: at least one positive and one negative fixture for each accepted choice/facet group should
  be compared against JDK XML Schema validation where practical; `TASK-0025` readiness accepts that
  evidence for the implemented choice and facet subsets.
- Native Image: new `0.2.0` fixtures join representative smoke coverage in their implementation
  tasks. Native Image conformance breadth remains a later `TASK-0044` concern.
- Release posture: readiness evidence must not create a `0.1.0` release tag or claim publication
  readiness without a separate release-engineering task. `TASK-0025` also does not create a `0.2.0`
  release tag or publication claim.

## `0.3.0` verification evidence

`TASK-0026` defined planned verification for `XP-XSD10-COMPOSED`. `TASK-0027` added executable
evidence for named model groups and attribute groups, `TASK-0028` added executable evidence for
named list/union simple types, `TASK-0029` added executable evidence for initial derivation
flattening, and `TASK-0030` records readiness evidence for the composed slice.

- Group support: `T-GROUP-*` and `T-ATTRGROUP-*` frontend, IR, binding, generated source,
  reader/writer/validator, deterministic emission, generated compile, unsupported diagnostics,
  conformance, interop, and selected Native Image smoke evidence are part of `TASK-0027`
  acceptance.
- Simple type composition: `T-LIST-*` and `T-UNION-*` frontend, IR, binding, generated source,
  lexical reader/writer/validator behavior, unsupported diagnostics, conformance, and interop
  comparisons are part of `TASK-0028` acceptance.
- Derivation support: `T-DERIVATION-*` frontend, IR, binding, generated source, reader/writer/
  validator behavior, cycle diagnostics, unsupported diagnostics, conformance, and interop
  comparisons are part of `TASK-0029` acceptance.
- Composed conformance/interop: `T-CONF-XP-XSD10-COMPOSED-*` and `T-INTEROP-COMPOSED-*` fixtures
  include positive and negative cases compared against JDK XML Schema validation where practical.
- Native Image: representative composed-schema generated-code paths for group/attribute-group,
  list/union, and derivation behavior join the smoke lane as implementation evidence; broader
  Native Image conformance remains a later `TASK-0044` concern.

## `0.4.0` semantic verification evidence

`TASK-0031` defines planned verification for `XP-XSD10-SEMANTIC`. `TASK-0032` adds implementation
evidence for the accepted nillable/default/fixed subset; `TASK-0033` adds implementation evidence
for the accepted direct substitution-group subset; remaining semantic implementation evidence
belongs to `TASK-0034`, and readiness evidence belongs to `TASK-0035`.

- Nillable/default/fixed support: `T-SEMANTIC-NIL-*`, `T-SEMANTIC-DEFAULT-*`, and
  `T-SEMANTIC-FIXED-*` frontend, IR, binding, generated source, reader/writer/validator,
  deterministic emission, generated compile, unsupported diagnostics, conformance, and interop
  comparisons are executable `TASK-0032` evidence.
- Substitution group support: `T-SUBSTITUTION-*` frontend, IR, binding, generated source,
  reader/writer/validator, graph diagnostics, unsupported diagnostics, conformance, interop
  comparisons, and generated-code smoke coverage are executable `TASK-0033` evidence.
- Semantic validation: `T-SEMANTIC-VALIDATION-*` generated validation source, object/XML validation,
  diagnostic ordering, fixed-value checks, nil-content checks, substitution dispatch diagnostics,
  conformance, and interop comparisons.
- Semantic conformance/interop: `T-CONF-XP-XSD10-SEMANTIC-*` and `T-INTEROP-SEMANTIC-*` fixtures
  should include positive and negative cases compared against JDK XML Schema validation where
  practical.
- Native Image: representative semantic generated-code paths should join the generated-code smoke
  lane in implementation tasks; broader Native Image conformance remains a later `TASK-0044`
  concern.
