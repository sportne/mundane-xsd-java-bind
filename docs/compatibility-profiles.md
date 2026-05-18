# Compatibility profiles

## XML/schema profiles

| Profile | Name | Meaning | Phase |
|---|---|---|---|
| `XP-DATA-10` | XSD 1.0 data-structure subset | Simple elements, complex types, attributes, nested elements, sequences, optional/repeated elements, namespaces, includes/imports, generated model/reader/writer/basic structural validation, and lexical conversion for the currently supported scalar types. | 1 |
| `XP-DATA-10-CHOICE` | Data subset with choices | Opt-in `0.2.0` extension for local singleton `xs:choice` particles with local or referenced supported element branches, generated sealed choice model types, reader/writer support, and explicit diagnostics for out-of-scope model-group shapes. | 2 |
| `XP-VALIDATION-10-BASIC` | Basic generated validation | Opt-in `0.2.0` extension for named simple-type restrictions using accepted enumeration, string length, numeric inclusive range, and string pattern facets over already supported scalar bases. | 2 |
| `XP-XSD10-COMPOSED` | Composed XSD 1.0 schemas | Opt-in `0.3.0` profile composing the accepted data, choice, and validation subsets; `TASK-0027` adds accepted named model group and attribute group flattening, `TASK-0028` adds accepted named list/union simple types, and `TASK-0029` adds accepted initial derivation flattening. | 3 |
| `XP-XSD10-SEMANTIC` | XSD 1.0 semantic expansion | Planned opt-in `0.4.0` profile composing `XP-XSD10-COMPOSED` with accepted `nillable`, `default`, `fixed`, direct substitution group, and semantic validation behavior. `TASK-0031` documents this token; the public API token is not added until `TASK-0032`. | 4 |
| `XP-XSD10-FULL` | Full XSD 1.0 | Substitution groups, full derivation semantics, wildcards, identity constraints, nillable, default/fixed, mixed content. | Future |
| `XP-XSD11-ASSERT` | XSD 1.1 assertions | XSD 1.1 features including assertions and conditional alternatives. | Future |
| `XP-XML11` | XML 1.1 | XML 1.1 parsing/serialization compatibility. | Future |
| `XP-INTEROP` | Interoperability | Round-trip and validation comparison with existing XML tooling. | Ongoing |

## Java/runtime profiles

| Profile | Meaning |
|---|---|
| `JP-21-MAIN` | Java 21 source/release baseline. |
| `JP-25-COMPAT` | Compatibility lane using Java 25. |
| `RT-CORE-ZERO-THIRD-PARTY` | Generated code plus runtime core have no third-party dependencies. |
| `RT-JDKXML-ADAPTER` | Optional adapter using JDK XML APIs behind project-owned interfaces. |
| `NI-SMOKE` | Native Image smoke tests for generated bindings. |
| `NI-CONFORMANCE` | Native Image execution of selected round-trip/conformance tests. |

## Build profiles

| Profile | Meaning |
|---|---|
| `BUILD-ONLINE` | Normal developer build with remote repositories. |
| `BUILD-OFFLINE-HYDRATED` | Offline build using hydrated local Maven repo and provisioned Gradle distribution. |
| `BUILD-CI-MATRIX` | CI on Java 21 and Java 25. |
| `BUILD-STRICT` | Full quality, architecture, coverage, docs, generated-code, and native-image gates. |

## `0.2.0` Readiness Baseline

`TASK-0025` accepts the Practical Data Contracts readiness evidence but does not create a release
tag or publication claim. `TASK-0023` accepts `XP-DATA-10-CHOICE` as an opt-in profile; default
`XP-DATA-10` still rejects `xs:choice`. `TASK-0024` accepts `XP-VALIDATION-10-BASIC` as an opt-in
profile; default `XP-DATA-10` still rejects restricted simple-type facets. Schemas that require both
choice and facet support remain out of scope until a future profile-composition task accepts that
behavior.

The `XP-DATA-10-CHOICE` implementation scope is limited to local `xs:choice` particles in a complex
type, either as the only content particle or inside an existing supported sequence. Accepted choices
use `minOccurs` of `0` or `1`, `maxOccurs` of `1`, and singleton local or referenced element
branches whose resolved types are already supported by `XP-DATA-10`.

The `XP-VALIDATION-10-BASIC` implementation scope is limited to named `xs:simpleType` restrictions
over `xs:string`, `xs:boolean`, `xs:int`, `xs:integer`, `xs:long`, and `xs:decimal`, with accepted
enumeration, string length, numeric inclusive range, and string pattern facets. Full model groups,
list/union simple types, derivation chains, full datatype semantics, and `XP-XSD10-FULL` remain
future work.

## `0.3.0` Readiness Baseline

`TASK-0026` accepted `XP-XSD10-COMPOSED` as a planning profile. `TASK-0027` added the public API
token and generator behavior for accepted named model groups and attribute groups; `TASK-0028` added
accepted named list/union simple types; `TASK-0029` added accepted initial derivation flattening;
`TASK-0030` records the readiness evidence. These tasks do not add dependency metadata, a release
tag, or a publication claim. The default profile remains `XP-DATA-10`, and the standalone `0.2.0`
choice and facet profiles remain narrower.

`XP-XSD10-COMPOSED` composes the accepted `XP-DATA-10`, `XP-DATA-10-CHOICE`, and
`XP-VALIDATION-10-BASIC` behavior with these `0.3.0` additions:

- accepted in `TASK-0027`: global `xs:group` declarations containing one `xs:sequence` of already-supported
  particles, direct singleton `xs:group ref` use, global `xs:attributeGroup` declarations containing
  supported attributes, and direct `xs:attributeGroup ref` use. Accepted groups are flattened into
  the containing model in deterministic order.
- accepted in `TASK-0028`: named `xs:list` simple types whose `itemType` resolves to a supported
  scalar built-in or accepted named restricted scalar alias, with required singleton elements and
  required attributes bound as immutable `List<T>` values; named `xs:union` simple types whose
  `memberTypes` resolve to supported scalar built-ins or accepted named restricted scalar aliases,
  with generated lexical `String` fields and explicit member validation.
- accepted in `TASK-0029`: named complex-type `xs:complexContent/xs:extension` flattening, with
  base fields before derived fields and no generated Java inheritance, plus named simple
  restriction derivation chains over supported scalar bases with merged accepted facet metadata.

Repeated or optional group references, nested model groups beyond the accepted direct shape,
`xs:all`, wildcards, anonymous list/union member types, optional or repeated list-valued XML fields,
nested list/union composition, `simpleContent`, complex restriction, mixed content, abstract types,
substitution groups, identity constraints, defaults/fixed semantics, and full XSD 1.0 conformance
remain out of scope with explicit diagnostics.

## `0.4.0` Planning Baseline

`TASK-0031` accepts planned opt-in profile `XP-XSD10-SEMANTIC` for follow-on implementation tasks.
This planning task does not add the public API token or generator behavior. `TASK-0032` is the first
approved implementation task to add the public token across API, CLI, Gradle plugin, and
CoreGenerator if this planning gate is accepted.

`XP-XSD10-SEMANTIC` composes `XP-XSD10-COMPOSED` with these planned `0.4.0` additions:

- planned for `TASK-0032`: `nillable="true"` only for required singleton elements with already
  supported non-list value types, bound as `Optional<T>` where `Optional.empty()` represents
  explicit `xsi:nil`; scalar `default` and `fixed` values only for supported built-ins or accepted
  restricted scalar aliases. Present empty simple elements may use element defaults; absent optional
  elements remain absent. Absent attributes with defaults or fixed values are read as effective
  model values.
- planned for `TASK-0033`: direct global `xs:element substitutionGroup="head"` members and
  singleton head references only. Generated models use an explicit sealed branch type with one
  record branch per accepted head or member element and preserve the actual element name for
  reader/writer dispatch.
- planned for `TASK-0034`: generated validation for accepted semantic behavior, including nil
  content rules, fixed-value checks, default/fixed reader behavior, substitution dispatch
  diagnostics, deterministic diagnostic ordering, and interop comparison.

Optional or repeated nillable fields, nillable attributes, complex/list/union defaults,
ambiguous nil/default/fixed combinations, repeated substitution groups, nested substitution groups,
substitution cycles, blocking/final semantics, full polymorphism, abstract complex types, wildcards,
mixed content, identity constraints, full datatype semantics, full derivation semantics, XSD 1.1,
artifact publication, and full XSD 1.0 conformance remain out of scope with explicit diagnostics.
