# mundane XSD Java Binding

**Status:** Design-Control Pack v0.1 with the first supported `XP-DATA-10` generator vertical slice,
accepted `0.2.0` Practical Data Contracts readiness evidence, accepted `0.3.0`
`XP-XSD10-COMPOSED` readiness evidence, accepted `0.4.0` `XP-XSD10-SEMANTIC`
readiness evidence, and accepted `0.5.0` `XP-XSD10-DOCUMENT` document/open-content
readiness evidence, and accepted `0.6.0` hardening evidence through selected conformance,
benchmark, Native Image, and release dry-run lanes. The repository includes generated model,
reader, writer, validator, runtime-core, optional JDK XML adapters, a public generator API, a CLI
`generate` command, a Gradle plugin for the accepted subsets, representative round-trip examples,
Native Image smoke coverage, and local publication dry-run validation.

`mundane XSD Java Binding` is a schema-to-code generator and runtime architecture for Java. It is conceptually adjacent to JAXB/Jakarta XML Binding, but it is deliberately designed as a modern, explicit, generated-code system with strong engineering controls and GraalVM Native Image friendliness from the beginning.

## Development model

This project is largely coding-agent driven. Human maintainers set the direction, review the design-control documents, and approve implementation gates; coding agents are expected to do much of the scaffold, documentation, build, and eventually implementation work under the rules in `AGENT.md`.

That workflow is intentional, so the repository is structured to be explicit about requirements, architecture, verification, build behavior, and task handoffs. Contributor-facing documentation is part of the product, not an afterthought.

## Project mission

Generate Java 21 model, XML reader, XML writer, and validation code from XML Schema documents. Generated code must be explicit, statically analyzable, readable, deterministic, and suitable for Native Image without runtime reflection-based binding.

## Hard constraints

- Schema-to-code only.
- No code-to-schema generation.
- Avoid annotation-based runtime behavior.
- Prefer generated serializers/deserializers over reflection.
- Runtime core and generated code must not require third-party dependencies.
- Dependencies are allowed in the generator, build infrastructure, tests, and tooling.
- Design, requirements, architecture, verification, and infrastructure must be accepted before product implementation begins.

## Implemented `XP-DATA-10` slice

The first implementation phase supports schemas that primarily define XML data-structure types:

- simple elements
- complex types
- attributes
- nested elements
- sequences
- optional and repeated elements
- imports/includes
- namespaces
- generated Java model types
- generated XML writer/marshaller
- generated XML reader/unmarshaller
- basic generated validation for required content, sequence order, cardinality, and common scalar lexical values
- round-trip tests
- CLI and Gradle plugin generation entry points
- Native Image smoke tests

Opt-in `0.2.0` profiles now cover the accepted local singleton `xs:choice` subset and practical
named simple-type facets for enumeration, string length, numeric inclusive range, and string
pattern validation.

The opt-in `0.3.0` `XP-XSD10-COMPOSED` profile composes the accepted `XP-DATA-10`,
`XP-DATA-10-CHOICE`, and `XP-VALIDATION-10-BASIC` behavior with accepted named model group and
attribute-group flattening, named list/union simple types, and initial derivation flattening.
Generated models keep explicit code shapes: flattened group and extension fields in deterministic
order, required singleton list values as immutable `List<T>`, and union values as lexical `String`.

The opt-in `0.4.0` `XP-XSD10-SEMANTIC` profile adds accepted `nillable`, scalar `default`,
scalar `fixed`, direct substitution-group semantics, and expanded generated validation for those
accepted semantic paths. Required singleton nillable elements bind as `Optional<T>`, where
`Optional.empty()` represents explicit `xsi:nil`; defaulted/fixed scalar attributes are read as
effective model values; accepted substitution head references bind as sealed branch models that
preserve actual XML element names; generated readers, writers, and validators remain explicit and
reflection-free.

The opt-in `0.5.0` `XP-XSD10-DOCUMENT` profile adds accepted direct `xs:any`
wildcard/open-content support inside sequences with explicit `processContents="skip"`, accepted
`mixed="true"` complex types with sequence content, and stable project XML serialization policy
evidence. Accepted wildcard fields bind as immutable `List<XmlFragment>` values; generated readers
retain expanded names, attributes, text, and nested element fragments, while writers and validators
handle those fragments without DOM or reflection. Accepted mixed types expose generated content-list
models that preserve non-whitespace text, known elements, and wildcard fragments in source order;
whitespace-only mixed text is dropped.

Full simple type semantics, repeated or optional list-valued XML fields, full derivation semantics,
full substitution group semantics, wildcard shapes beyond the accepted direct `xs:any` subset,
`xs:anyAttribute`, `processContents="lax"` or `"strict"`, wildcard choices, mixed choices,
comments or processing instruction retention, entity-reference identity, XML Canonicalization,
identity constraints, full XSD 1.0 conformance, and XSD 1.1 remain future-profile work.

## Repository entry points

- `DESIGN_CONTROL_PACK_v0.1.md` — pack manifest and acceptance checklist.
- `AGENT.md` — binding rules for coding agents.
- `docs/charter.md` — project charter.
- `docs/build/README.md` — contributor-facing build, offline, and toolchain notes.
- `docs/requirements/` — requirements taxonomy and phase-one requirements.
- `docs/architecture/` — architecture and module boundaries.
- `docs/verification/` — verification and validation strategy.
- `docs/adr/` — initial architectural decisions.
- `docs/agent/handoff.md` — next-step task sequence for coding agents.

## Build note

This project is configured for Gradle 9.5.1 with Groovy DSL. The standard Gradle wrapper scripts and `gradle/wrapper/gradle-wrapper.jar` are committed. Start with `docs/build/README.md`; for fully offline builds, provision the Gradle distribution and local Maven repository as described in `docs/build/offline-build.md`.

Common commands after hydrating the wrapper and dependencies:

```bash
./gradlew help
./gradlew projects
./gradlew validateDesignControlPack
./gradlew qualityGate
./gradlew nativeSmoke
./gradlew printPublishedArtifacts
./gradlew -Pmxjb.version=0.6.0-alpha.0 publicationDryRun
```

`nativeSmoke` requires a GraalVM installation with `native-image`; the documented local path for
the current environment is in `docs/verification/native-image-test-plan.md`.

`publicationDryRun` stages candidate Maven artifacts under `build/staging-repository` and validates
metadata without publishing remotely, signing artifacts, creating a release tag, or committing a
version bump.

Generate Java sources for an approved schema subset with:

```bash
./gradlew :modules:generator-cli:run --args="generate --schema ${PWD}/examples/purchase-order/src/main/resources/schema/purchase-order.xsd --output ${PWD}/build/generated/mxjb-readme"
```

Gradle builds can use the plugin id `io.github.mundanej.mxjb` and configure explicit schema inputs:

```groovy
plugins {
    id 'java'
    id 'io.github.mundanej.mxjb'
}

mxjb {
    schema('src/main/resources/schema/order.xsd')
    namespacePackage('urn:orders', 'com.example.orders')
}
```

Build conventions live in `build-logic/` as composable Gradle convention plugins. Published code modules live under `modules/`; non-published examples live under `examples/`.
