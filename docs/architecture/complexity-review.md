# Complexity and architecture review

## TASK-0069 findings

`TASK-0069` reviewed the current full-profile implementation using the deletion test: if removing a
class or subsystem would delete a large amount of behavior but leave few isolated concepts behind,
the boundary is probably too shallow.

The largest reviewed implementation surfaces are:

| Surface | Approx. lines | Essential complexity | Accidental complexity |
|---|---:|---|---|
| `SchemaIrBuilder` + normalization helpers | 3,266 before `TASK-0084` | XSD symbol spaces, profile gates, normalization, deterministic diagnostics. | `TASK-0084` extracts occurrence/cardinality, QName, cardinality-composition, and diagnostic ordering policy; `TASK-0090` extracts particle flattening/cardinality and wildcard namespace/process policy; `TASK-0091` extracts derivation restriction/final-control diagnostics and identity XPath subset parsing; resource-aware lookup, profile policy, component normalization, and derivation flattening remain future tranches. |
| `BindingModelBuilder` + binding planners | 1,571 before `TASK-0085` | Java naming, product-scope binding shapes, validation plan construction. | `TASK-0085` extracts deterministic naming/package allocation and mixed/grouped content-list planning; `TASK-0092` extracts substitution-group and declared-base dynamic `xsi:type` branch planning; schema lookup, type-reference binding, and validation metadata remain future tranche candidates. |
| `GeneratedReaderEmitter` + emitter plans | 2,430 before `TASK-0086` | Generated source must be explicit, deterministic, reflection-free, and location-aware. | `TASK-0086` introduces root source/helper/field traversal plans; `TASK-0087` extracts reader helper-state and scalar-conversion planning; diagnostic text, element traversal, and Java formatting still live in source assembly. |
| `GeneratedValidatorEmitter` + emitter plans | 2,652 before `TASK-0086` | Object/XML validation, identity tables, datatype/facet checks, and deterministic diagnostics. | `TASK-0086` introduces root source/helper/field traversal plans; `TASK-0089` extracts validator traversal and identity-constraint activation plans; datatype/facet snippets and identity helper algorithms remain inside source assembly. |
| `GeneratedWriterEmitter` + emitter plans | 934 before `TASK-0086` | Deterministic XML output for generated and retained content. | `TASK-0086` names shared root source/helper/field traversal planning; `TASK-0088` extracts writer field/content branch traversal plans; scalar text formatting and Java formatting remain inside source assembly. |
| `XmlDatatypes` + datatype helpers | 842 before `TASK-0093` | Dependency-free XML Schema lexical parsing/formatting in runtime-core. | `TASK-0093` extracts lexical whitespace/patterns, numeric/range helpers, date-time lexical validation, QName/NOTATION helpers, binary helpers, and list-token helpers while preserving the public facade. |
| `W3cXsd10SuiteIntake` + `W3cXsd10BindingExecutor` | 953 before `TASK-0083` | Dependency-free W3C metadata classification and mapped-row execution. | `TASK-0083` separates generated-binding execution from suite intake; report writing remains a smaller follow-on extraction candidate. |

## Prioritized simplification plan

1. Extract an IR normalization policy layer from `SchemaIrBuilder`.
   - `TASK-0084` starts this extraction with package-private `SchemaIrNormalizationPolicy` for
     occurrence/cardinality parsing, QName lexical resolution, cardinality composition, and
     diagnostic creation/sorting.
   - `TASK-0090` adds package-private particle and wildcard normalization helpers for nested
     sequence flattening, group-reference cardinality wrapping, attribute namespace policy,
     wildcard ambiguity inputs, wildcard namespace/process policy, and anyAttribute namespace
     union.
   - `TASK-0091` adds package-private derivation and identity normalization helpers for
     final-control diagnostics, complex-restriction diagnostics, and accepted identity
     selector/field XPath subset parsing.
   - Remaining candidate boundaries: resource-aware lookup, profile policy, component
     normalization, and derivation flattening.
   - Test strategy: keep `SchemaIrBuilderTest`, `SchemaIrDeltaHardeningTest`, and focused policy
     tests as behavior locks; no behavior changes in extraction tranches.

2. Split binding naming from binding shape construction.
   - `TASK-0085` extracts package-private `BindingNameAllocator` and `BindingContentPlanner`.
   - `BindingNameAllocator` owns namespace-to-package derivation, type-name suffix allocation,
     field-name collision helpers, and binding configuration diagnostics.
   - `BindingContentPlanner` owns mixed/grouped content-list field planning, grouped branch
     positions, wildcard branch metadata, and composed branch cardinality helpers.
   - `TASK-0092` extracts package-private `BindingSubstitutionPlanner` and
     `BindingDynamicTypePlanner` for direct substitution-group and declared-base dynamic
     `xsi:type` branch planning.
   - Remaining candidate boundaries: schema lookup, type-reference binding, and validation-rule
     metadata.
   - Test strategy: keep focused allocator/planner tests plus `BindingModelBuilderTest` and
     generated-code smoke as behavior locks; no output changes are expected from this tranche.

3. Introduce emitter planning objects before text emission.
   - `TASK-0086` introduces package-private `GeneratedEmitterPlan`, `GeneratedEmitterKind`, and
     `GeneratedEmitterFieldPlan`.
   - The first plan boundary captures root element/type metadata, generated source names and paths,
     root helper names, and binding-order field traversal inputs before source text assembly.
   - `TASK-0087` adds package-private reader state and scalar-conversion plans for helper feature
     flags, scalar parse expressions, datatype list item names, and datatype helper class literals.
   - `TASK-0088` adds package-private writer field traversal and content branch traversal plans.
   - `TASK-0089` adds package-private validator traversal and root identity-constraint plans.
   - Remaining candidate boundaries: content-position traversal plans, datatype/facet emission
     snippets, and identity helper algorithm assembly.
   - Test strategy: keep focused plan tests plus existing generated-source compile and smoke tests
     as end-to-end guards; no generated source output change is expected from this tranche.

4. Group runtime datatype families behind small helpers.
   - `TASK-0093` extracts package-private lexical whitespace/pattern helpers, numeric/range
     helpers, date/time lexical helpers, QName/NOTATION helpers, binary helpers, and list-token
     helpers.
   - Leverage: reduces risk when adding datatype edge cases without changing the runtime-core
     public API or dependency policy.
   - Remaining candidate boundary: focused facet-rule objects if future datatype changes make the
     generated-validator interaction more frequent.
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
  shape; future work should split writer, validator, content traversal, and identity planning
  instead of attempting one broad rewrite.
- The safest first task is binding name allocation because it has clear inputs/outputs and aligns
  with the scheduled `TASK-0075` naming review.
- Any extraction that changes generated output must explain golden diffs and keep generated-code
  compile, smoke, conformance, and Native Image evidence current.
