# Compatibility profiles

## XML/schema profiles

| Profile | Name | Meaning | Phase |
|---|---|---|---|
| `XP-DATA-10` | XSD 1.0 data-structure subset | Simple elements, complex types, attributes, nested elements, sequences, optional/repeated elements, namespaces, includes/imports, generated model/reader/writer/basic structural validation, and lexical conversion through the shared XSD 1.0 datatype engine for accepted scalar positions. | 1 |
| `XP-DATA-10-CHOICE` | Data subset with choices | Opt-in `0.2.0` extension for local singleton `xs:choice` particles with local or referenced supported element branches, generated sealed choice model types, reader/writer support, and explicit diagnostics for out-of-scope model-group shapes. | 2 |
| `XP-VALIDATION-10-BASIC` | Basic generated validation | Opt-in `0.2.0` extension for named simple-type restrictions using accepted enumeration, string length, numeric inclusive range, and string pattern facets over already supported scalar bases. | 2 |
| `XP-XSD10-COMPOSED` | Composed XSD 1.0 schemas | Opt-in `0.3.0` profile composing the accepted data, choice, and validation subsets; `TASK-0027` adds accepted named model group and attribute group flattening, `TASK-0028` adds accepted named list/union simple types, and `TASK-0029` adds accepted initial derivation flattening. | 3 |
| `XP-XSD10-SEMANTIC` | XSD 1.0 semantic expansion | Opt-in `0.4.0` profile composing `XP-XSD10-COMPOSED` with accepted `nillable`, scalar `default`, scalar `fixed`, direct substitution-group behavior, and expanded generated validation for those accepted semantic paths from `TASK-0032` through `TASK-0034`. | 4 |
| `XP-XSD10-DOCUMENT` | Document-oriented and open content | Opt-in `0.5.0` profile composing `XP-XSD10-SEMANTIC`; `TASK-0037` adds accepted direct `xs:any` wildcard retention, `TASK-0038` adds accepted mixed-content ordering, and `TASK-0039` verifies stable project serialization policy. | 5 |
| `XP-XSD10-FULL` | Full XSD 1.0 | Planned full XML Schema 1.0 support for this binding generator, tracked by `docs/verification/xsd10-full-feature-matrix.md`; the public token exists but generation is intentionally rejected until implementation gates accept it. | Planned |
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
| `BUILD-RELEASE-DRY-RUN` | Local publication staging and metadata validation without remote publication, signing, tags, or version bumps. |

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
list/union simple type composition, derivation chains, and `XP-XSD10-FULL` execution remain
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

`TASK-0051` adds required `xs:all` groups, all-optional `xs:all` groups, repeated element-only
choices, nested singleton sequences, and single-particle repeated/optional group refs with composed
cardinality. Optional all-groups with required children, repeated/optional multi-particle groups,
wildcard choice branches, anonymous list/union member types, optional or repeated list-valued XML
fields, nested list/union composition, mixed choice content, identity-constraint edge cases, strict/lax
schema-known wildcard deep validation, and full XSD 1.0 conformance remain out of scope with
explicit diagnostics or future-study classification.

`TASK-0050` broadens the datatype engine used by existing executable profiles without making
`XP-XSD10-FULL` executable. Accepted scalar element and attribute positions now map all XML Schema
1.0 primitive and derived built-ins to exact Java/runtime values where needed, including temporal
values, duration, binary values, anyURI, QName/NOTATION, float/double special values, bounded
numeric families, and list-valued built-ins such as `NMTOKENS`, `IDREFS`, and `ENTITIES`.
Generated readers, writers, and validators use the same dependency-free runtime datatype engine.
`TASK-0052` adds local/global attribute namespace qualification, prohibited attributes, retained
`xs:anyAttribute` values as `List<XmlAttribute>`, wildcard namespace-token handling, and
`processContents` metadata. `TASK-0053` adds accepted simpleContent text-with-attributes binding,
basic complexContent restriction member checks, and selected derivation/substitution diagnostics.
Grouped content-list models, complete derivation algebra, strict/lax schema-known wildcard deep
validation, `xsi:type` dispatch, and identity-constraint edge-case coverage remain future tasks.

## `0.4.0` Semantic Baseline

`TASK-0031` accepted planned opt-in profile `XP-XSD10-SEMANTIC`; `TASK-0032` added the public
API token plus accepted nillable/default/fixed behavior; `TASK-0033` added accepted direct
substitution-group behavior; and `TASK-0034` verified expanded generated validation for the
accepted semantic paths.

`XP-XSD10-SEMANTIC` composes `XP-XSD10-COMPOSED` with these `0.4.0` additions:

- accepted in `TASK-0032`: `nillable="true"` only for required singleton elements with already
  supported non-list value types, bound as `Optional<T>` where `Optional.empty()` represents
  explicit `xsi:nil`; scalar `default` and `fixed` values only for supported built-ins or accepted
  restricted scalar aliases. Present empty simple elements may use element defaults; absent optional
  elements remain absent. Absent attributes with defaults or fixed values are read as effective
  model values.
- accepted in `TASK-0033`: direct global `xs:element substitutionGroup="head"` members and
  singleton head references. Generated models use an explicit sealed branch type with one record
  branch per accepted concrete head or member element and preserve the actual element name for
  reader/writer dispatch.
- accepted in `TASK-0053`: abstract substitution heads with concrete members, nested substitution
  members, and repeated head references for accepted branch shapes. Generated readers and writers
  dispatch by actual XML element name; validators recurse through concrete branch values.
- accepted in `TASK-0034`: generated validation hardening for accepted semantic behavior,
  including nil content rules, fixed-value checks, default/fixed reader behavior, substitution
  dispatch diagnostics, deterministic diagnostic ordering, unsupported validation-category
  diagnostics, and interop comparison.

At the `0.4.0` readiness gate, optional or repeated nillable fields, nillable attributes,
complex/list/union defaults, ambiguous nil/default/fixed combinations, blocking/final semantics,
`xsi:type` polymorphism, abstract complex types outside substitution-head dispatch, wildcards,
mixed content, identity constraints, full derivation semantics, artifact publication, and full
XSD 1.0 conformance remained out of scope with explicit diagnostics. Later full-XSD gates add
selected support for several of those areas without making `XP-XSD10-FULL` executable yet.

## `0.5.0` Document-Oriented Baseline

`TASK-0036` accepted planned opt-in profile `XP-XSD10-DOCUMENT` for document-oriented and
open-content support. `TASK-0037` adds the public API token and accepted direct wildcard behavior,
`TASK-0038` adds accepted mixed-content behavior, `TASK-0039` verifies stable project
serialization policy, and `TASK-0040` records readiness evidence. This does not add dependency
metadata, a release tag, or a publication claim.

`XP-XSD10-DOCUMENT` composes `XP-XSD10-SEMANTIC` with these `0.5.0` additions:

- accepted in `TASK-0037`: direct `xs:any` particles inside accepted sequences only, with
  `processContents="skip"` and deterministic namespace constraints `##any`, `##other`, `##local`,
  `##targetNamespace`, or explicit namespace URI tokens. Accepted wildcard fields bind as immutable
  `List<XmlFragment>` values using dependency-free runtime-core fragment values rather than DOM.
  Retained fragments preserve expanded names, attributes, text, and nested element fragments; exact
  lexical prefixes, comments, processing instructions, and entity-reference identity remain out of
  scope.
- accepted in `TASK-0038`: `mixed="true"` only for complex types with accepted sequence content.
  Generated models preserve non-whitespace text, known element, and accepted wildcard-fragment
  order through an explicit generated content-list sealed type. Whitespace-only text is dropped
  deterministically.
- accepted in `TASK-0039`: stable project serialization behavior for generated writers, retained
  unknown XML fragments, namespace prefix assignment, controlled attribute ordering, and text
  escaping. Generated writers emit attributes before child content, ordinary content in binding
  order, repeated values in list order, and mixed-content branches in content-list order. Retained
  fragments emit `XmlFragment` attributes and content in stored list order. This is not XML
  Canonicalization and does not support cryptographic canonical XML claims.

Wildcards in choices, substitution branches, and unsupported group/derivation edge cases remain out
of scope. `xs:anyAttribute` is retained as `List<XmlAttribute>` for accepted shapes, and
`processContents="lax"` or `"strict"` metadata is preserved, but full schema-known deep validation
for lax/strict remains future work. Mixed choices, comments or processing instruction preservation,
entity-reference semantics, DOM-backed binding, identity-constraint edge cases beyond accepted
generated model shapes, full derivation semantics, artifact publication, and full XSD 1.0
conformance remain out of scope with
explicit diagnostics.

## `0.6.0` Hardening Readiness Baseline

`TASK-0041` accepted `0.6.0` as a hardening and release maturity slice rather than a schema-feature
slice. `TASK-0042` adds selected local conformance/interop fixture classification and unsupported
diagnostic evidence. `TASK-0043` adds advisory generated-binding benchmark baselines.
`TASK-0044` adds selected Native Image conformance wiring and records a local `native-image`
toolchain blocker when unavailable. `TASK-0045` adds local publication dry-run staging and metadata
validation. `TASK-0046` reconciles these evidence lanes as final readiness evidence.

The accepted `0.6.0` posture keeps `qualityGate` JVM-focused and leaves benchmark, Native Image,
and publication dry-run lanes explicit and opt-in. It does not add product behavior, dependency
metadata, signing, remote publication, release tags, full XSD 1.0 conformance, XSD 1.1 support,
XML 1.1 support, XML Canonicalization, XML Signature canonical forms, lexical prefix preservation,
or hard performance guarantees.

## `XP-XSD10-FULL` Planning Baseline

`TASK-0048` opens the full XML Schema 1.0 program without making the profile executable. The public
`GeneratorProfile.XP_XSD10_FULL` token exists so API, CLI, docs, and future task cards can refer to
one stable target; `CoreGenerator` still rejects that profile until implementation tasks accept
support. The planned sequence is:

- accepted in `TASK-0049`: complete XSD 1.0 frontend and component-model awareness for schema
  defaults, annotations, notations, direct and transitive chameleon include namespace adoption with
  conflict diagnostics, remaining symbol spaces, and deterministic pre-binding diagnostics for
  known-but-later XSD 1.0 constructs.
- accepted in `TASK-0050`: complete XSD 1.0 datatype and facet engine for accepted schema shapes,
  while keeping `XP-XSD10-FULL` non-executable.
- accepted in `TASK-0051`: full content-model compiler expansion for accepted shapes.
- accepted in `TASK-0052`: full attributes and wildcards expansion for accepted shapes.
- accepted in `TASK-0053`: derivation, substitution, and dynamic typing expansion for accepted shapes.
- accepted in `TASK-0054`: identity constraints and document-level validation for accepted shapes.
- accepted in `TASK-0055`: opt-in full-suite W3C XML Schema 1.0 intake and classification
  harness, with no generated-binding support claim for unmapped W3C rows.
- accepted in `TASK-0056`: final readiness reconciliation for the current sequence. The evidence
  keeps `XP-XSD10-FULL` planned and non-executable because the feature matrix still has full-XSD
  blockers and the W3C suite intake has zero generated-binding-supported rows.

XSD 1.1 and XML 1.1 are not compatibility profiles or future project targets. A real publication
workflow remains separate release-engineering work.

## `1.0.0` Full-XSD Release Bar

`TASK-0058` rejects a stable-subset interpretation for `1.0.0`. The `1.0.0` compatibility posture
requires executable `XP-XSD10-FULL` generated-binding support plus W3C generated-binding evidence.
The public token remains non-executable until `TASK-0065`.

The planned blocker sequence is `TASK-0059` through `TASK-0066`: grouped content-list models,
content-model automata and UPA, derivation/dynamic typing, strict/lax wildcard deep validation,
remaining datatype/nil/identity edges, W3C generated-binding row mapping, full-profile enablement,
and final release workflow/readiness. Until those gates are accepted, public docs must keep full
XSD 1.0 and `1.0.0` release claims out of scope.
