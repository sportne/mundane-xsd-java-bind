# Coding-agent handoff

This file gives the next exact sequence of tasks. Agents must not skip ahead to product implementation.

## Current repository state

- Design-Control Pack v0.1 scaffold exists and phase-one readiness has accepted the initial `XP-DATA-10` requirement baseline.
- Initial `generator-core` schema resource-resolution, syntax frontend, component graph, normalized IR, binding model planning, deterministic generated-model/generated-writer/generated-reader/generated-validator source emission, generated-source verification harness, active generator-core coverage enforcement, representative round-trip example/conformance fixtures, and `runtime-core` primitives are present.
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
18. `TASK-0018` through `TASK-0021`: Draft backlog for the first public vertical slice. Not approved for implementation until each prior gate is accepted.
19. `TASK-0022` through `TASK-0046`: Draft post-0.1.0 vertical-slice backlog. Not approved for implementation until each slice planning task is accepted.

`TASK-0018` is the next draft task, but it is not approved for implementation until the generator API and CLI vertical-slice scope is reviewed and explicitly promoted.

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
| `TASK-0018` | 5 | draft | Public generator API and CLI vertical slice. |
| `TASK-0019` | 5 | draft | Gradle plugin vertical slice. |
| `TASK-0020` | 5 | draft | Native Image and quality-gate hardening. |
| `TASK-0021` | 5 | draft | First public vertical slice release-readiness review. |

## Post-0.1.0 draft vertical-slice backlog

Each post-0.1.0 slice must include interop evidence where practical. Interop is a recurring verification expectation, not a final cleanup phase.

| Task | Version slice | Status | Purpose |
|---|---|---|---|
| `TASK-0022` | 0.2.0 | draft | Plan Practical Data Contracts support. |
| `TASK-0023` | 0.2.0 | draft | Implement feasible `xs:choice` support. |
| `TASK-0024` | 0.2.0 | draft | Expand practical simple restrictions. |
| `TASK-0025` | 0.2.0 | draft | Practical Data Contracts readiness review. |
| `TASK-0026` | 0.3.0 | draft | Plan Composed XSD 1.0 schema support. |
| `TASK-0027` | 0.3.0 | draft | Implement named model groups and attribute groups. |
| `TASK-0028` | 0.3.0 | draft | Implement accepted simple type composition. |
| `TASK-0029` | 0.3.0 | draft | Implement initial derivation support. |
| `TASK-0030` | 0.3.0 | draft | Composed XSD 1.0 readiness review. |
| `TASK-0031` | 0.4.0 | draft | Plan XSD 1.0 semantic expansion. |
| `TASK-0032` | 0.4.0 | draft | Implement `nillable`, `default`, and `fixed` semantics. |
| `TASK-0033` | 0.4.0 | draft | Implement accepted substitution group support. |
| `TASK-0034` | 0.4.0 | draft | Expand validation semantics for the accepted feature set. |
| `TASK-0035` | 0.4.0 | draft | XSD 1.0 semantic expansion readiness review. |
| `TASK-0036` | 0.5.0 | draft | Plan document-oriented and open-content support. |
| `TASK-0037` | 0.5.0 | draft | Implement accepted wildcard/open-content support. |
| `TASK-0038` | 0.5.0 | draft | Implement accepted mixed-content support. |
| `TASK-0039` | 0.5.0 | draft | Add canonicalization and serialization-policy tests. |
| `TASK-0040` | 0.5.0 | draft | Document-oriented/open-content readiness review. |
| `TASK-0041` | 0.6.0 | draft | Plan hardening and release maturity. |
| `TASK-0042` | 0.6.0 | draft | Expand ongoing interop/conformance harness. |
| `TASK-0043` | 0.6.0 | draft | Add performance, memory, and streaming benchmarks. |
| `TASK-0044` | 0.6.0 | draft | Harden Native Image conformance lane. |
| `TASK-0045` | 0.6.0 | draft | Release engineering and publication readiness. |
| `TASK-0046` | 0.6.0 | draft | Hardening and release maturity readiness review. |

## Next implementation gate

Implementation for `TASK-0018` remains blocked until it is promoted from draft to approved. The next readiness review should confirm:

- `TASK-0014` JDK XML adapters are the accepted optional bridge from JDK StAX to `runtime-core` interfaces for tests and examples.
- `TASK-0015` generated readers are the accepted source-emission baseline for constructing generated models from `runtime-core` `XmlEventReader` input.
- `TASK-0016` generated validators are the accepted source-emission baseline for returning `ValidationResult` values for supported model and XML inputs.
- `TASK-0017` accepted representative purchase-order and multi-namespace round-trip examples, selected conformance fixtures, and example Native Image smoke evidence.
- `TASK-0018` must add the public generator API and CLI vertical slice only; it must not add Gradle plugin behavior.
- Later round-trip lanes should reuse the generated-source harness, generated readers/writers, and `runtime-jdkxml` adapters instead of introducing separate XML adapter mechanics.
