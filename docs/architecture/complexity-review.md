# Complexity and architecture review

## TASK-0069 findings

`TASK-0069` reviewed the current full-profile implementation using the deletion test: if removing a
class or subsystem would delete a large amount of behavior but leave few isolated concepts behind,
the boundary is probably too shallow.

The largest reviewed implementation surfaces are:

| Surface | Approx. lines | Essential complexity | Accidental complexity |
|---|---:|---|---|
| `SchemaIrBuilder` + `SchemaIrNormalizationPolicy` | 3,266 before `TASK-0084` | XSD symbol spaces, profile gates, normalization, deterministic diagnostics. | `TASK-0084` extracts occurrence/cardinality, QName, cardinality-composition, and diagnostic ordering policy; resource-aware lookup, profile policy, component normalization, derivation flattening, wildcard composition, and identity paths remain future tranches. |
| `BindingModelBuilder` + binding planners | 1,571 before `TASK-0085` | Java naming, product-scope binding shapes, validation plan construction. | `TASK-0085` extracts deterministic naming/package allocation and mixed/grouped content-list planning; substitution/dynamic branch binding, schema lookup, type-reference binding, and validation metadata remain future tranche candidates. |
| `GeneratedReaderEmitter` + emitter plans | 2,430 before `TASK-0086` | Generated source must be explicit, deterministic, reflection-free, and location-aware. | `TASK-0086` introduces root source/helper/field traversal plans; state-machine decisions, diagnostic text, and Java formatting still live in source assembly. |
| `GeneratedValidatorEmitter` + emitter plans | 2,652 before `TASK-0086` | Object/XML validation, identity tables, datatype/facet checks, and deterministic diagnostics. | `TASK-0086` introduces root source/helper/field traversal plans; validation traversal, identity helpers, and emitted Java snippets remain future tranche candidates. |
| `GeneratedWriterEmitter` + emitter plans | 934 before `TASK-0086` | Deterministic XML output for generated and retained content. | `TASK-0086` names shared root source/helper/field traversal planning; scalar/content write traversal remains inside source assembly. |
| `XmlDatatypes` | 842 | Dependency-free XML Schema lexical parsing/formatting in runtime-core. | Many datatype families share whitespace, bounds, and lexical-special-case mechanics inside one utility. |
| `W3cXsd10SuiteIntake` + `W3cXsd10BindingExecutor` | 953 before `TASK-0083` | Dependency-free W3C metadata classification and mapped-row execution. | `TASK-0083` separates generated-binding execution from suite intake; report writing remains a smaller follow-on extraction candidate. |

## Prioritized simplification plan

1. Extract an IR normalization policy layer from `SchemaIrBuilder`.
   - `TASK-0084` starts this extraction with package-private `SchemaIrNormalizationPolicy` for
     occurrence/cardinality parsing, QName lexical resolution, cardinality composition, and
     diagnostic creation/sorting.
   - Remaining candidate boundaries: content-particle normalization, attribute/wildcard
     normalization, derivation normalization, and identity-constraint normalization.
   - Test strategy: keep `SchemaIrBuilderTest`, `SchemaIrDeltaHardeningTest`, and focused policy
     tests as behavior locks; no behavior changes in extraction tranches.

2. Split binding naming from binding shape construction.
   - `TASK-0085` extracts package-private `BindingNameAllocator` and `BindingContentPlanner`.
   - `BindingNameAllocator` owns namespace-to-package derivation, type-name suffix allocation,
     field-name collision helpers, and binding configuration diagnostics.
   - `BindingContentPlanner` owns mixed/grouped content-list field planning, grouped branch
     positions, wildcard branch metadata, and composed branch cardinality helpers.
   - Remaining candidate boundaries: substitution branch planning, `xsi:type` dynamic branch
     planning, type-reference binding, and validation-rule metadata.
   - Test strategy: keep focused allocator/planner tests plus `BindingModelBuilderTest` and
     generated-code smoke as behavior locks; no output changes are expected from this tranche.

3. Introduce emitter planning objects before text emission.
   - `TASK-0086` introduces package-private `GeneratedEmitterPlan`, `GeneratedEmitterKind`, and
     `GeneratedEmitterFieldPlan`.
   - The first plan boundary captures root element/type metadata, generated source names and paths,
     root helper names, and binding-order field traversal inputs before source text assembly.
   - Remaining candidate boundaries: content-position traversal plans, scalar conversion plans,
     reader state-machine plans, writer content traversal plans, validator object/XML traversal
     plans, and identity-helper plans.
   - Test strategy: keep focused plan tests plus existing generated-source compile and smoke tests
     as end-to-end guards; no generated source output change is expected from this tranche.

4. Group runtime datatype families behind small helpers.
   - Candidate boundary: lexical whitespace helpers, numeric range helpers, date/time helpers,
     QName/NOTATION helpers, and list-token helpers.
   - Leverage: reduces risk when adding datatype edge cases without changing the runtime-core
     public API or dependency policy.
   - Test strategy: keep `RuntimePrimitivesTest`, `XmlDatatypesDeltaTest`, and generated datatype
     conformance fixtures as behavior locks.

5. Separate W3C suite intake classification from generated-binding execution.
   - `TASK-0083` extracts generated-binding execution into `W3cXsd10BindingExecutor` while
     preserving intake classification, report shape, and mapped-row behavior.
   - Remaining candidate boundary: report writer extraction after more mapped rows make report
     evolution frequent.
   - Test strategy: keep `W3cXsd10SuiteIntakeTest` and `W3cXsd10SuiteIntakeDeltaTest` as behavior
     locks for parse/classification/report rows and mapped binding execution.

## ADR and risk notes

- No ADR conflict is visible for these refactors because they preserve module boundaries,
  dependency policy, no-reflection generated code, and Native Image posture.
- The highest-risk remaining refactor is deeper emitter planning because it touches generated source
  shape; future work should split reader, writer, validator, scalar conversion, content traversal,
  and identity planning instead of attempting one broad rewrite.
- The safest first task is binding name allocation because it has clear inputs/outputs and aligns
  with the scheduled `TASK-0075` naming review.
- Any extraction that changes generated output must explain golden diffs and keep generated-code
  compile, smoke, conformance, and Native Image evidence current.
