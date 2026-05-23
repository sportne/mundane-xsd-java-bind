# Coding-agent handoff

This file gives the next exact sequence of tasks. Agents must not skip ahead to product implementation.

## Current repository state

- Design-Control Pack v0.1 scaffold exists and phase-one readiness has accepted the initial `XP-DATA-10` requirement baseline.
- Initial `generator-core` schema resource-resolution, syntax frontend, component graph, normalized IR, binding model planning, deterministic generated-model/generated-writer/generated-reader/generated-validator source emission, generated-source verification harness, active generator-core coverage enforcement, representative round-trip example/conformance fixtures, public generator API/CLI/Gradle plugin vertical slices, ArchUnit architecture-rule hardening, Native Image smoke aggregate, `XP-XSD10-COMPOSED` named model group/attribute group, accepted list/union simple type support, accepted initial derivation flattening, accepted Composed XSD 1.0 readiness evidence, accepted XSD 1.0 semantic expansion planning, accepted `XP-XSD10-SEMANTIC` nillable/default/fixed semantics, accepted direct substitution group support, accepted expanded semantic validation evidence, accepted XSD 1.0 semantic expansion readiness evidence, accepted document-oriented/open-content planning, accepted `XP-XSD10-DOCUMENT` direct wildcard/open-content support, accepted mixed-content support, accepted serialization-policy evidence, accepted document-oriented/open-content readiness evidence, accepted hardening/release maturity planning, accepted selected interop/conformance harness expansion, accepted benchmark baseline evidence, accepted selected Native Image conformance hardening, accepted release-engineering dry-run readiness, accepted final hardening/release maturity readiness evidence, accepted `XP-XSD10-FULL` standards reset and feature matrix planning, accepted full XSD 1.0 frontend/component-model awareness, accepted full XSD 1.0 datatype/facet engine support for executable profile shapes, and `runtime-core` primitives are present.
- Branding is settled as `mundane XSD Java Binding`, with Java root package `io.github.mundanej.mxjb`, Maven group `io.github.mundanej`, and `mxjb-*` artifact IDs.
- Gradle 9.5.1 module structure, quality tooling, dependency verification, dependency locking, offline helper scripts, CI skeleton, ADRs, and documentation scaffolds exist.

## Task sequence

1. `TASK-0001`: Validate Design-Control Pack v0.1 file presence and consistency. Completed for the scaffold.
2. `TASK-0002`: Hydrate and verify the Gradle wrapper and dependency metadata. Completed for the scaffold; repeat when dependencies change.
3. `TASK-0003`: Run and harden Gradle quality-gate wiring without product code. Completed for the scaffold.
4. `TASK-0004`: Convert staged build policies into failing gates where meaningful. Completed for implemented modules; empty modules retain explicit staged skips until they contain compiled production classes.
5. `TASK-0005`: Perform phase-one readiness review and open implementation task cards. Completed for the accepted phase-one baseline.
6. `TASK-0006`: Implement the schema resource-resolution vertical slice in `generator-core`. Completed and accepted.
7. `TASK-0007`: Implement the XSD syntax frontend subset in `generator-core`. Completed and accepted.
8. `TASK-0008`: Implement the component graph and normalized IR in `generator-core`. Completed and accepted.
9. `TASK-0009`: Implement binding model planning for names, packages, fields, and validation shape. Completed and accepted.
10. `TASK-0010`: Implement `runtime-core` public primitives and XML event/output interfaces. Completed and accepted.
11. `TASK-0011`: Implement deterministic generated Java model source emission. Completed and accepted.
12. `TASK-0012`: Implement deterministic generated XML writer source emission. Completed and accepted.
13. `TASK-0013`: Implement generated-source verification harness, golden fixtures, JVM smoke, and generated-code Native Image smoke. Completed and accepted.
14. `TASK-0014`: Implement `runtime-jdkxml` adapters for generated-code tests and examples. Completed and accepted.
15. `TASK-0015`: Implement generated XML reader source emission. Completed and accepted.
16. `TASK-0016`: Implement basic generated validation and diagnostics. Completed and accepted.
17. `TASK-0017`: Round-trip examples and conformance fixture expansion. Completed and accepted.
18. `TASK-0018`: Public generator API and CLI vertical slice. Completed and accepted.
19. `TASK-0047`: Ad hoc ArchUnit architecture-rule hardening. Completed and accepted.
20. `TASK-0019`: Implement the Gradle plugin vertical slice. Completed and accepted.
21. `TASK-0020`: Harden Native Image and quality-gate evidence for the public vertical slice. Completed and accepted.
22. `TASK-0021`: First public vertical slice release-readiness review. Completed and accepted.
23. `TASK-0022`: Plan Practical Data Contracts support. Completed and accepted.
24. `TASK-0023`: Implement feasible `xs:choice` support. Completed and accepted.
25. `TASK-0024`: Expand practical simple restrictions. Completed and accepted.
26. `TASK-0025`: Practical Data Contracts readiness review. Completed and accepted.
27. `TASK-0026`: Plan Composed XSD 1.0 schema support. Completed and accepted.
28. `TASK-0027`: Implement named model groups and attribute groups. Completed and accepted.
29. `TASK-0028`: Implement accepted simple type composition. Completed and accepted.
30. `TASK-0029`: Implement initial derivation support. Completed and accepted.
31. `TASK-0030`: Composed XSD 1.0 readiness review. Completed and accepted.
32. `TASK-0031`: Plan XSD 1.0 semantic expansion. Completed and accepted.
33. `TASK-0032`: Implement `nillable`, `default`, and `fixed` semantics. Completed and accepted.
34. `TASK-0033`: Implement accepted substitution group support. Completed and accepted.
35. `TASK-0034`: Expand validation semantics for the accepted `0.4.0` feature set. Completed and accepted.
36. `TASK-0035`: XSD 1.0 semantic expansion readiness review. Completed and accepted.
37. `TASK-0036`: Plan document-oriented and open-content support. Completed and accepted.
38. `TASK-0037`: Implement accepted wildcard/open-content support. Completed and accepted.
39. `TASK-0038`: Implement accepted mixed-content support. Completed and accepted.
40. `TASK-0039`: Add canonicalization and serialization-policy tests. Completed and accepted.
41. `TASK-0040`: Document-oriented/open-content readiness review. Completed and accepted.
42. `TASK-0041`: Plan hardening and release maturity. Completed and accepted.
43. `TASK-0042`: Expand ongoing interop/conformance harness. Completed and accepted.
44. `TASK-0043`: Add performance, memory, and streaming benchmarks. Completed and accepted.
45. `TASK-0044`: Harden Native Image conformance lane. Completed and accepted.
46. `TASK-0045`: Release engineering and publication readiness. Completed and accepted.
47. `TASK-0046`: Final hardening/release maturity readiness review. Completed and accepted.
48. `TASK-0048`: Standards reset and full XSD 1.0 feature matrix. Completed and accepted.
49. `TASK-0049`: Complete XSD 1.0 frontend and component model. Completed and accepted.
50. `TASK-0050`: Complete XSD 1.0 datatype and facet engine. Completed and accepted.
51. `TASK-0051`: Expand accepted XSD 1.0 content-model support toward the full compiler. Completed and accepted.
52. `TASK-0052`: Expand accepted XSD 1.0 attributes and wildcards toward the full compiler. Completed and accepted.
53. `TASK-0053`: Expand accepted XSD 1.0 derivation, substitution, and dynamic typing. Completed and accepted.
54. `TASK-0054`: Implement XSD 1.0 identity constraints and document-level validation. Next implementation gate.
55. `TASK-0055`: Add full XSD 1.0 conformance-suite intake. Draft.
56. `TASK-0056`: Full XSD 1.0 readiness review. Draft.

`TASK-0027` has accepted named model group and attribute group support for `XP-XSD10-COMPOSED`
without adding release tags or publication claims. `TASK-0028` has accepted named list/union simple
type support for `XP-XSD10-COMPOSED` without adding release tags or publication claims.
`TASK-0029` has accepted initial derivation flattening for `XP-XSD10-COMPOSED` without adding
release tags or publication claims. `TASK-0030` has accepted the `0.3.0` Composed XSD 1.0
readiness review without adding release tags or publication claims. `TASK-0031` has accepted
`0.4.0` XSD 1.0 semantic expansion planning without adding product behavior, dependency metadata,
release tags, or publication claims. `TASK-0032` has accepted `XP-XSD10-SEMANTIC`
nillable/default/fixed behavior without adding dependency metadata, release tags, or publication
claims. `TASK-0033` has accepted direct substitution group behavior without adding dependency metadata,
release tags, or publication claims. `TASK-0034` has accepted expanded semantic validation
hardening without adding dependency metadata, release tags, or publication claims. `TASK-0035` has
accepted the `0.4.0` XSD 1.0 semantic expansion readiness review without adding product behavior,
dependency metadata, release tags, or publication claims. `TASK-0036` has accepted `0.5.0`
document-oriented/open-content planning without adding product behavior, dependency metadata,
release tags, or publication claims. `TASK-0037` has accepted direct wildcard/open-content support
without adding dependency metadata, release tags, or publication claims. `TASK-0038` has accepted
mixed-content support without adding dependency metadata, release tags, or publication claims.
`TASK-0039` has accepted serialization-policy evidence without adding dependency metadata, release
tags, publication claims, or formal XML Canonicalization claims. `TASK-0040` has accepted
document-oriented/open-content readiness evidence without adding product behavior, dependency
metadata, release tags, publication claims, full XSD 1.0 conformance claims, or formal XML
Canonicalization claims. `TASK-0041` has accepted `0.6.0` hardening and release maturity planning
without adding product behavior, dependency metadata, generated output, release tags, publication
workflows, quality-gate weakening, or unsupported conformance claims. `TASK-0042` has accepted
selected local interop/conformance harness expansion without adding schema-feature behavior,
dependencies, broad external suite vendoring, release claims, or quality-gate weakening.
`TASK-0043` has accepted advisory benchmark baseline evidence without adding dependencies, product
behavior, hard performance guarantees, release claims, release tags, or `qualityGate`
requirements. `TASK-0044` has accepted selected Native Image conformance hardening without adding
dependencies, product behavior, reflection configuration, release claims, release tags, or
`qualityGate` requirements. `TASK-0045` has accepted release-engineering dry-run readiness with
local staged publication validation, approved Maven coordinates, Gradle plugin marker evidence,
release-note non-claims, and no remote publication, signing, version bump, or release tag.
`TASK-0046` has accepted the final `0.6.0` hardening/release maturity readiness review.
`TASK-0048` has accepted the standards reset for the `XP-XSD10-FULL` program, removed active
XSD 1.1/XML 1.1 future-profile targets, added the public planned `XP-XSD10-FULL` token with
deterministic non-executable generator behavior, and added the full XSD 1.0 feature matrix plus
draft follow-on task cards. `TASK-0049` has accepted frontend/component-model awareness for
remaining XSD 1.0 schema constructs while keeping unimplemented behavior as deterministic
pre-binding diagnostics. `TASK-0050` has accepted full XSD 1.0 built-in datatype and facet support
for currently executable schema shapes while keeping `XP-XSD10-FULL` non-executable. `TASK-0051`
has accepted the content-model compiler expansion for legal `xs:all`, nested singleton sequences,
single-particle repeated/optional groups with composed cardinality, repeated generated choices, and
deterministic diagnostics for non-flattenable repeated/optional multi-particle groups and optional
`xs:all` groups with required children.
`TASK-0052` has accepted attribute and wildcard expansion for local/global attribute namespace
qualification, `use="prohibited"`, retained `xs:anyAttribute` values, wildcard namespace-token
handling, and `processContents` metadata while keeping strict/lax schema-known deep validation and
derivation interactions for later gates. `TASK-0053` has accepted simpleContent
text-with-attributes binding, repeated/nested/abstract substitution heads, deterministic
substitution cycle diagnostics, and basic complex restriction member checks while leaving full
`xsi:type`, block/final, and complete restriction algebra for later full-XSD gates.
`TASK-0054` is the next implementation gate.

## Draft completion backlog

The draft backlog covers the project charter's first success milestone: CLI or Gradle generation for supported `XP-DATA-10` data-structure schemas, generated model/reader/writer/basic validation code, Java 21 compilation, representative round trips, and Native Image smoke tests.

| Task | Phase | Status | Purpose |
|---|---|---|---|
| `TASK-0006` | 2 | accepted | Schema resource resolution and resolved-schema manifest. |
| `TASK-0007` | 2 | accepted | XSD syntax frontend for the supported data-structure subset. |
| `TASK-0008` | 2 | accepted | Component graph, QName resolution, and normalized schema IR. |
| `TASK-0009` | 2 | accepted | Binding model planning for names, packages, fields, and validation shape. |
| `TASK-0010` | 3 | accepted | `runtime-core` public primitives and XML event/output interfaces. |
| `TASK-0011` | 3 | accepted | Generated immutable Java model source emission. |
| `TASK-0012` | 3 | accepted | Generated XML writer source emission. |
| `TASK-0013` | 3 | accepted | Generated-source compile, golden, determinism, JVM smoke, and Native Image smoke harness. |
| `TASK-0014` | 4 | accepted | JDK XML adapters for generated-code tests and examples. |
| `TASK-0015` | 4 | accepted | Generated XML reader source emission. |
| `TASK-0016` | 4 | accepted | Basic generated validation and diagnostics. |
| `TASK-0017` | 4 | accepted | Round-trip examples and conformance fixture expansion. |
| `TASK-0018` | 5 | accepted | Public generator API and CLI vertical slice. |
| `TASK-0047` | ad hoc | accepted | ArchUnit architecture-rule hardening. |
| `TASK-0019` | 5 | accepted | Gradle plugin vertical slice. |
| `TASK-0020` | 5 | accepted | Native Image and quality-gate hardening. |
| `TASK-0021` | 5 | accepted | First public vertical slice release-readiness review. |

## Post-0.1.0 draft vertical-slice backlog

Each post-0.1.0 slice must include interop evidence where practical. Interop is a recurring verification expectation, not a final cleanup phase.

| Task | Version slice | Status | Purpose |
|---|---|---|---|
| `TASK-0022` | 0.2.0 | accepted | Plan Practical Data Contracts support. |
| `TASK-0023` | 0.2.0 | accepted | Implement feasible `xs:choice` support. |
| `TASK-0024` | 0.2.0 | accepted | Expand practical simple restrictions. |
| `TASK-0025` | 0.2.0 | accepted | Practical Data Contracts readiness review. |
| `TASK-0026` | 0.3.0 | accepted | Plan Composed XSD 1.0 schema support. |
| `TASK-0027` | 0.3.0 | accepted | Implement named model groups and attribute groups. |
| `TASK-0028` | 0.3.0 | accepted | Implement accepted simple type composition. |
| `TASK-0029` | 0.3.0 | accepted | Implement initial derivation support. |
| `TASK-0030` | 0.3.0 | accepted | Composed XSD 1.0 readiness review. |
| `TASK-0031` | 0.4.0 | accepted | Plan XSD 1.0 semantic expansion. |
| `TASK-0032` | 0.4.0 | accepted | Implement `nillable`, `default`, and `fixed` semantics. |
| `TASK-0033` | 0.4.0 | accepted | Implement accepted substitution group support. |
| `TASK-0034` | 0.4.0 | accepted | Expand validation semantics for the accepted feature set. |
| `TASK-0035` | 0.4.0 | accepted | XSD 1.0 semantic expansion readiness review. |
| `TASK-0036` | 0.5.0 | accepted | Plan document-oriented and open-content support. |
| `TASK-0037` | 0.5.0 | accepted | Implement accepted wildcard/open-content support. |
| `TASK-0038` | 0.5.0 | accepted | Implement accepted mixed-content support. |
| `TASK-0039` | 0.5.0 | accepted | Add canonicalization and serialization-policy tests. |
| `TASK-0040` | 0.5.0 | accepted | Document-oriented/open-content readiness review. |
| `TASK-0041` | 0.6.0 | accepted | Plan hardening and release maturity. |
| `TASK-0042` | 0.6.0 | accepted | Expand ongoing interop/conformance harness. |
| `TASK-0043` | 0.6.0 | accepted | Add performance, memory, and streaming benchmarks. |
| `TASK-0044` | 0.6.0 | accepted | Harden Native Image conformance lane. |
| `TASK-0045` | 0.6.0 | accepted | Release engineering and publication readiness. |
| `TASK-0046` | 0.6.0 | accepted | Hardening and release maturity readiness review. |
| `TASK-0048` | XSD 1.0 full | accepted | Standards reset and full XSD 1.0 feature matrix. |
| `TASK-0049` | XSD 1.0 full | accepted | Complete XSD 1.0 frontend and component model. |
| `TASK-0050` | XSD 1.0 full | accepted | Complete XSD 1.0 datatype and facet engine. |
| `TASK-0051` | XSD 1.0 full | accepted | Expand accepted XSD 1.0 content-model support toward the full compiler. |
| `TASK-0052` | XSD 1.0 full | accepted | Expand accepted XSD 1.0 attributes and wildcards toward the full compiler. |
| `TASK-0053` | XSD 1.0 full | accepted | Expand accepted XSD 1.0 derivation, substitution, and dynamic typing. |
| `TASK-0054` | XSD 1.0 full | next | Implement XSD 1.0 identity constraints and document-level validation. |
| `TASK-0055` | XSD 1.0 full | draft | Add full XSD 1.0 conformance-suite intake. |
| `TASK-0056` | XSD 1.0 full | draft | Full XSD 1.0 readiness review. |

## Current implementation gate

`TASK-0054` is the next implementation gate after the accepted `TASK-0053`
derivation/substitution expansion. It must implement XSD 1.0 identity constraints and
document-level validation before conformance-suite intake or final full-XSD readiness work begins.
Future product behavior, dependency changes, release publication, signing, tags, benchmark
thresholds, or broader schema support still require an accepted task card or ADR.

Any future work must preserve:

- `TASK-0014` JDK XML adapters are the accepted optional bridge from JDK StAX to `runtime-core` interfaces for tests and examples.
- `TASK-0015` generated readers are the accepted source-emission baseline for constructing generated models from `runtime-core` `XmlEventReader` input.
- `TASK-0016` generated validators are the accepted source-emission baseline for returning `ValidationResult` values for supported model and XML inputs.
- `TASK-0017` accepted representative purchase-order and multi-namespace round-trip examples, selected conformance fixtures, and example Native Image smoke evidence.
- `TASK-0018` accepted the public generator API and CLI vertical slice only; `TASK-0019` is the first approved place to add Gradle plugin behavior after its gate is promoted.
- `TASK-0019` accepted the public Gradle plugin id `io.github.mundanej.mxjb`, `mxjb` extension, and cacheable `generateMxjbSources` task backed by the public generator pipeline.
- `TASK-0020` accepted the root `nativeSmoke` aggregate and native CI command
  `./gradlew validateDesignControlPack nativeSmoke --console=plain` while keeping `qualityGate`
  JVM-focused.
- `TASK-0021` accepted the first public vertical slice readiness evidence, verified requirement
  statuses for the implemented `XP-DATA-10` slice, and kept `xs:choice`, simple-type facets,
  derivation, wildcards, mixed content, and identity constraints as future-profile work.
- `TASK-0022` accepted the `0.2.0` Practical Data Contracts planning scope without creating a
  `0.1.0` release tag or publication claim. `TASK-0023` accepted local singleton `xs:choice`
  particles with supported element branches behind opt-in profile `XP-DATA-10-CHOICE`, including
  conformance/interop fixtures and representative generated-code smoke coverage. `TASK-0024`
  accepted named simple-type restrictions for enumeration, string length, numeric inclusive range,
  and string pattern facets over already supported scalar bases behind opt-in profile
  `XP-VALIDATION-10-BASIC`, including conformance/interop fixtures and representative generated-code
  smoke coverage. `TASK-0025` accepted the Practical Data Contracts readiness review, confirmed
  support claims match the implemented choice and facet evidence, and kept the repository in
  readiness-only posture without a `0.1.0` or `0.2.0` release tag.
- `TASK-0026` accepted planned opt-in profile `XP-XSD10-COMPOSED` for the `0.3.0` Composed XSD 1.0
  slice. The accepted implementation sequence is `TASK-0027` named model groups and attribute
  groups, `TASK-0028` named list/union simple types, `TASK-0029` initial derivation flattening, and
  `TASK-0030` readiness review. The scope remains narrower than full XSD 1.0.
- `TASK-0027` accepted public profile `XP-XSD10-COMPOSED` and implemented named model groups and
  attribute groups by flattening accepted refs before binding. Default `XP-DATA-10` and the narrower
  choice/facet profiles remain unchanged. Positive and negative composed conformance fixtures compare
  JDK XML Schema validation with generated bindings, and generated-code smoke fixtures exercise a
  representative composed path for the Native Image smoke lane.
- `TASK-0028` accepted named `xs:list` simple types with supported scalar or restricted scalar alias
  `itemType`, required singleton list-valued elements/attributes bound as immutable `List<T>`, named
  `xs:union` simple types with supported scalar or restricted scalar alias `memberTypes`, and
  lexical `String` union fields with generated member validation. Optional/repeated list-valued
  fields, anonymous list/union members, and nested list/union composition remain out of scope.
- `TASK-0029` accepted named complex-type `xs:complexContent/xs:extension` flattened before binding
  with base fields before derived fields and no generated Java inheritance, plus named simple
  restriction derivation chains over accepted scalar restrictions with merged facet metadata.
  `simpleContent`, complex restriction, abstract types, substitution groups, mixed content, and full
  derivation semantics remain out of scope.
- `TASK-0030` accepted `0.3.0` `XP-XSD10-COMPOSED` readiness evidence, confirmed support claims
  cover only accepted named model group/attribute-group, list/union, and initial derivation
  behavior, recorded conformance/interop and representative generated-code Native Image evidence,
  kept full XSD 1.0 conformance out of scope, and introduced no release tag or publication claim.
- `TASK-0031` accepted planned opt-in profile `XP-XSD10-SEMANTIC` for `0.4.0` semantic expansion
  without adding the public API token or generator behavior. `TASK-0032` accepted the public profile
  token and implemented accepted `nillable`, `default`, and `fixed` semantics with generated model,
  reader, writer, validator, conformance, interop, and generated-code smoke evidence. `TASK-0033`
  accepted direct substitution groups with sealed branch models, reader/writer dispatch, validator
  traversal, unsupported diagnostics, conformance, interop, and generated-code smoke evidence.
  `TASK-0034` accepted expanded semantic validation hardening for deterministic object diagnostics,
  nil/fixed XML diagnostics, substitution branch validation, unsupported validation-category
  diagnostics, conformance, interop, and generated-code smoke evidence. `TASK-0035` accepted the
  semantic expansion readiness review and confirmed support claims, conformance/interop evidence,
  Native Image evidence, and release posture.
  Identity constraints, wildcards, mixed content, full derivation semantics, release tags, and
  publication claims remain out of scope.
- `TASK-0036` accepted planned opt-in profile `XP-XSD10-DOCUMENT` for `0.5.0`
  document-oriented/open-content support without adding the public API token or generator behavior.
  `TASK-0037` accepted the public profile token and direct `xs:any` particles inside accepted
  sequences, `processContents="skip"` only, deterministic namespace constraints, immutable
  `List<XmlFragment>` wildcard fields, dependency-free runtime-core retained-fragment values,
  generated reader/writer/validator behavior, unsupported diagnostics, and conformance/interop
  evidence. `TASK-0038` accepted mixed-content behavior with generated sealed content-list models,
  non-whitespace text preservation, whitespace-only text dropping, ordered reader/writer behavior,
  generated validation, and conformance evidence. `TASK-0039` accepted stable project
  serialization-policy evidence for generated output, retained fragments, deterministic namespace
  prefix assignment, controlled attribute ordering, text/attribute escaping, secure reparse
  interop, and explicit non-claims for W3C XML Canonicalization or cryptographic canonical XML.
  `TASK-0040` accepted the `0.5.0` document/open-content readiness review, confirmed support
  claims, conformance/interop evidence, Native Image smoke coverage, release posture, and
  unsupported-document limitations. `TASK-0052` later added retained `xs:anyAttribute` support,
  broader wildcard namespace-token handling, and `processContents` metadata for accepted shapes.
  Full strict/lax schema-known wildcard validation, wildcard choices, substitution-branch
  wildcards, DOM-backed binding, parser-handle retention, comments/PI preservation,
  entity-reference semantics, identity constraints, full derivation semantics, release tags, and
  publication claims remain out of scope.
- `TASK-0047` accepted the architecture rule catalog and ArchUnit hardening categories that future production code must satisfy unless an ADR approves an exception.
- `TASK-0041` accepted `0.6.0` as a hardening and release maturity slice. The accepted sequence is
  `TASK-0042` selected conformance/interop expansion, `TASK-0043` benchmark baselines,
  `TASK-0044` selected Native Image conformance, `TASK-0045` release engineering dry-run
  readiness, and `TASK-0046` final readiness review. This planning task did not authorize release
  tags, artifact publication, dependency changes, quality-gate weakening, full XSD conformance
  claims, XML Canonicalization claims, or new product behavior.
- `TASK-0042` accepted selected local conformance/interop harness expansion with
  `selected-fixtures.tsv`, manifest classification tests, deterministic unsupported-diagnostic
  schemas, and manifest-linked executable conformance tests. It did not add schema-feature support,
  dependencies, broad external suite vendoring, release claims, or quality-gate weakening.
- `TASK-0043` accepted advisory benchmark baseline evidence with the explicit
  `./gradlew benchmarkSmoke --console=plain` lane, deterministic generated read/write/validate and
  document open-content/mixed-content workloads, and `docs/verification/performance-baselines.md`.
  It did not add dependencies, product behavior, hard performance guarantees, release claims,
  release tags, or `qualityGate` requirements.
- `TASK-0044` accepted selected Native Image conformance hardening with the explicit
  `./gradlew nativeConformance --console=plain` lane, build-time selected binding generation,
  static generated read/write/validate paths across supported profile families, selected
  unsupported-diagnostic checks, secure entity/resource denial evidence, and GraalVM workflow
  wiring. Local Native Image execution remains toolchain-dependent and records a concrete
  `native-image` blocker when unavailable. It did not add dependencies, product behavior,
  reflection configuration, release claims, release tags, or `qualityGate` requirements.
- `TASK-0045` accepted release-engineering dry-run readiness with the explicit
  `./gradlew -Pmxjb.version=0.6.0-alpha.0 publicationDryRun --console=plain` lane. It stages and
  validates the approved BOM, runtime, generator, Gradle plugin, plugin marker, and testing-support
  publications locally; adds release notes with supported profiles, evidence links, rollback, and
  explicit non-claims; keeps `gradle.properties` at `0.1.0-SNAPSHOT`; and does not add signing,
  secrets, remote repositories, release tags, artifact publication, dependencies, product behavior,
  or `qualityGate` requirements.
- `TASK-0046` accepted the final `0.6.0` hardening/release maturity readiness review. It reconciled
  selected conformance/interop, advisory benchmark, selected Native Image conformance, publication
  dry-run, security, unsupported-feature, release non-claim, and future-profile documentation;
  verified `REQ-QA-002`; recorded the local `native-image` blocker; and did not add product
  behavior, dependencies, release tags, artifact publication, signing, remote staging, quality-gate
  changes, or new schema support.
- `TASK-0048` accepted the full XSD 1.0 standards reset. `XP-XSD10-FULL` is now the only
  full-standard target, XSD 1.1 and XML 1.1 are not project targets, and
  `docs/verification/xsd10-full-feature-matrix.md` defines the remaining XSD 1.0 implementation
  surface. The public `GeneratorProfile.XP_XSD10_FULL` token is intentionally non-executable until
  `TASK-0049` through `TASK-0056` accept implementation evidence.
- `TASK-0049` accepted full XSD 1.0 frontend/component-model awareness. The syntax parser now
  preserves schema defaults, annotations, appinfo/documentation, notations, include/import/redefine
  nodes, `xs:all`, `xs:anyAttribute`, identity constraints, selector/field syntax, and
  abstract/block/final metadata. The component graph indexes notations and adopts including
  namespaces for direct and transitive chameleon includes with deterministic ambiguity/conflict
  diagnostics. Unimplemented constructs still fail before binding, and `XP-XSD10-FULL` remains
  non-executable.
- `TASK-0050` accepted the full XSD 1.0 datatype and facet engine for currently executable schema
  shapes. Runtime-core now owns exact dependency-free values for XML duration/date/time fragments,
  binary values, anyURI, and QName; generated scalar mapping covers all XML Schema 1.0 built-ins
  and derived built-ins; generated readers/writers/validators route lexical conversion and facets
  through the shared datatype engine; selected conformance compares datatype fixtures against JDK
  XML Schema validation. `XP-XSD10-FULL` remains non-executable until later structure, derivation,
  identity, and full-suite gates accept their evidence.
- Later round-trip and Native Image lanes should reuse the generator API/CLI/Gradle plugin, generated-source harness, generated readers/writers, and `runtime-jdkxml` adapters instead of introducing separate XML adapter mechanics.
