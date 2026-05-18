# Coding-agent handoff

This file gives the next exact sequence of tasks. Agents must not skip ahead to product implementation.

## Current repository state

- Design-Control Pack v0.1 scaffold exists and phase-one readiness has accepted the initial `XP-DATA-10` requirement baseline.
- Initial `generator-core` schema resource-resolution, syntax frontend, component graph, normalized IR, binding model planning, deterministic generated-model/generated-writer/generated-reader/generated-validator source emission, generated-source verification harness, active generator-core coverage enforcement, representative round-trip example/conformance fixtures, public generator API/CLI/Gradle plugin vertical slices, ArchUnit architecture-rule hardening, Native Image smoke aggregate, `XP-XSD10-COMPOSED` named model group/attribute group, accepted list/union simple type support, accepted initial derivation flattening, accepted Composed XSD 1.0 readiness evidence, accepted XSD 1.0 semantic expansion planning, accepted `XP-XSD10-SEMANTIC` nillable/default/fixed semantics, accepted direct substitution group support, accepted expanded semantic validation evidence, accepted XSD 1.0 semantic expansion readiness evidence, accepted document-oriented/open-content planning, accepted `XP-XSD10-DOCUMENT` direct wildcard/open-content support, and `runtime-core` primitives are present.
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
39. `TASK-0038`: Implement accepted mixed-content support. Current implementation gate.
40. `TASK-0039` through `TASK-0046`: Draft post-0.1.0 vertical-slice backlog. Not approved for implementation until each slice planning task or predecessor is accepted.

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
without adding dependency metadata, release tags, or publication claims. The next gate is `TASK-0038`.

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
| `TASK-0038` | 0.5.0 | draft | Implement accepted mixed-content support. |
| `TASK-0039` | 0.5.0 | draft | Add canonicalization and serialization-policy tests. |
| `TASK-0040` | 0.5.0 | draft | Document-oriented/open-content readiness review. |
| `TASK-0041` | 0.6.0 | draft | Plan hardening and release maturity. |
| `TASK-0042` | 0.6.0 | draft | Expand ongoing interop/conformance harness. |
| `TASK-0043` | 0.6.0 | draft | Add performance, memory, and streaming benchmarks. |
| `TASK-0044` | 0.6.0 | draft | Harden Native Image conformance lane. |
| `TASK-0045` | 0.6.0 | draft | Release engineering and publication readiness. |
| `TASK-0046` | 0.6.0 | draft | Hardening and release maturity readiness review. |

## Current implementation gate

`TASK-0038` is the current implementation gate. It implements the accepted mixed-content subset
planned by `TASK-0036` on top of the accepted `TASK-0037` wildcard baseline.
The remaining work must preserve:

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
  derivation, wildcards, mixed content, identity constraints, and XSD 1.1 as future-profile work.
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
  fields, anonymous list/union members, nested list/union composition, and full datatype semantics
  remain out of scope.
- `TASK-0029` accepted named complex-type `xs:complexContent/xs:extension` flattened before binding
  with base fields before derived fields and no generated Java inheritance, plus named simple
  restriction derivation chains over accepted scalar restrictions with merged facet metadata.
  `simpleContent`, complex restriction, abstract types, substitution groups, mixed content, full
  derivation semantics, and XSD 1.1 remain out of scope.
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
  Identity constraints, wildcards, mixed content, full datatype semantics, full derivation
  semantics, XSD 1.1, release tags, and publication claims remain out of scope.
- `TASK-0036` accepted planned opt-in profile `XP-XSD10-DOCUMENT` for `0.5.0`
  document-oriented/open-content support without adding the public API token or generator behavior.
  `TASK-0037` accepted the public profile token and direct `xs:any` particles inside accepted
  sequences, `processContents="skip"` only, deterministic namespace constraints, immutable
  `List<XmlFragment>` wildcard fields, dependency-free runtime-core retained-fragment values,
  generated reader/writer/validator behavior, unsupported diagnostics, and conformance/interop
  evidence. `TASK-0038` is the next implementation gate for accepted mixed-content behavior.
  `xs:anyAttribute`,
  `processContents="lax"` or `"strict"`, wildcard choices, substitution-branch wildcards,
  unsupported namespace constraints, DOM-backed binding, parser-handle retention, comments/PI
  preservation, entity-reference semantics, identity constraints, full datatype semantics, full
  derivation semantics, XSD 1.1, release tags, and publication claims remain out of scope.
- `TASK-0047` accepted the architecture rule catalog and ArchUnit hardening categories that future production code must satisfy unless an ADR approves an exception.
- Later round-trip and Native Image lanes should reuse the generator API/CLI/Gradle plugin, generated-source harness, generated readers/writers, and `runtime-jdkxml` adapters instead of introducing separate XML adapter mechanics.
