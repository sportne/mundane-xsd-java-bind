# Complexity and architecture review

## TASK-0069 findings

`TASK-0069` reviewed the current full-profile implementation using the deletion test: if removing a
class or subsystem would delete a large amount of behavior but leave few isolated concepts behind,
the boundary is probably too shallow.

The largest reviewed implementation surfaces are:

| Surface | Approx. lines | Essential complexity | Accidental complexity |
|---|---:|---|---|
| `SchemaIrBuilder` | 3,266 | XSD symbol spaces, profile gates, normalization, deterministic diagnostics. | One class owns resource-aware lookup, profile policy, component normalization, derivation flattening, wildcard composition, identity paths, and diagnostics. |
| `BindingModelBuilder` | 1,571 | Java naming, product-scope binding shapes, validation plan construction. | Naming allocation, field collision handling, content-list binding, wildcard binding, dynamic branch binding, and validation metadata are intertwined. |
| `GeneratedReaderEmitter` | 2,430 | Generated source must be explicit, deterministic, reflection-free, and location-aware. | Source text assembly mixes traversal planning, state-machine decisions, diagnostic text, and Java formatting. |
| `GeneratedValidatorEmitter` | 2,652 | Object/XML validation, identity tables, datatype/facet checks, and deterministic diagnostics. | Validation traversal planning and emitted Java snippets are difficult to inspect independently. |
| `GeneratedWriterEmitter` | 934 | Deterministic XML output for generated and retained content. | Smaller than reader/validator but repeats scalar/content traversal ideas that are not named as plans. |
| `XmlDatatypes` | 842 | Dependency-free XML Schema lexical parsing/formatting in runtime-core. | Many datatype families share whitespace, bounds, and lexical-special-case mechanics inside one utility. |
| `W3cXsd10SuiteIntake` | 953 | Dependency-free W3C metadata classification and mapped-row execution. | Classification policy, report writing, generated binding execution, JDK validation, and summary formatting live together. |

## Prioritized simplification plan

1. Extract an IR normalization policy layer from `SchemaIrBuilder`.
   - Candidate boundary: package-private normalizers for content particles, attributes/wildcards,
     derivation, identity constraints, and diagnostics.
   - Leverage: keeps schema lookup and profile gates visible while making each XSD construct easier
     to regression-test.
   - Test strategy: characterization tests around existing `SchemaIrBuilderTest` and
     `SchemaIrDeltaHardeningTest`; no behavior changes in the first extraction.

2. Split binding naming from binding shape construction.
   - Candidate boundary: package-private `BindingNameAllocator` and `BindingContentPlanner`.
   - Leverage: `TASK-0075` collision work can target a named allocator instead of exercising the
     whole binding builder for every edge.
   - Test strategy: move existing deterministic name and package-mapping tests into allocator-level
     tests before changing any output.

3. Introduce emitter planning objects before text emission.
   - Candidate boundary: reader, writer, and validator plans that describe fields, content
     positions, scalar conversions, and diagnostics before Java source strings are assembled.
   - Leverage: generated reader/validator behavior becomes reviewable without comparing long source
     snippets, and writer/reader/validator traversal decisions can share vocabulary.
   - Test strategy: snapshot the plan model in focused tests, then keep existing generated-source
     compile and smoke tests as end-to-end guards.

4. Group runtime datatype families behind small helpers.
   - Candidate boundary: lexical whitespace helpers, numeric range helpers, date/time helpers,
     QName/NOTATION helpers, and list-token helpers.
   - Leverage: reduces risk when adding datatype edge cases without changing the runtime-core
     public API or dependency policy.
   - Test strategy: keep `RuntimePrimitivesTest`, `XmlDatatypesDeltaTest`, and generated datatype
     conformance fixtures as behavior locks.

5. Separate W3C suite intake classification from generated-binding execution.
   - Candidate boundary: `W3cSuiteClassifier`, `W3cBindingMappingExecutor`, and report writer.
   - Leverage: `TASK-0070` can add mapped rows without coupling classification policy changes to
     execution mechanics.
   - Test strategy: keep `W3cXsd10SuiteIntakeTest` and `W3cXsd10SuiteIntakeDeltaTest`; add mapping
     executor characterization tests before broad row expansion.

## ADR and risk notes

- No ADR conflict is visible for these refactors because they preserve module boundaries,
  dependency policy, no-reflection generated code, and Native Image posture.
- The highest-risk refactor is emitter planning because it touches generated source shape; it should
  be split by reader, writer, and validator instead of attempted as one broad rewrite.
- The safest first task is binding name allocation because it has clear inputs/outputs and aligns
  with the scheduled `TASK-0075` naming review.
- Any extraction that changes generated output must explain golden diffs and keep generated-code
  compile, smoke, conformance, and Native Image evidence current.
