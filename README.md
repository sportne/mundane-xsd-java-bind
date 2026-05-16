# xsd-bind-java

**Status:** Design-Control Pack v0.1 scaffold. No XML schema compiler, runtime binding, reader, writer, validator, or generated-code implementation exists yet.

`xsd-bind-java` is a schema-to-code generator and runtime architecture for Java. It is conceptually adjacent to JAXB/Jakarta XML Binding, but it is deliberately designed as a modern, explicit, generated-code system with strong engineering controls and GraalVM Native Image friendliness from the beginning.

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

## First accepted implementation target

The first implementation phase targets schemas that primarily define XML data-structure types:

- simple elements
- complex types
- attributes
- nested elements
- sequences
- feasible choices
- optional and repeated elements
- practical simple type restrictions
- imports/includes
- namespaces
- generated Java model types
- generated XML writer/marshaller
- generated XML reader/unmarshaller
- round-trip tests
- basic validation or validation-ready architecture

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

This scaffold is configured for Gradle 9.5.1 with Groovy DSL. The standard Gradle wrapper scripts and `gradle/wrapper/gradle-wrapper.jar` are committed. Start with `docs/build/README.md`; for fully offline builds, provision the Gradle distribution and local Maven repository as described in `docs/build/offline-build.md`.

Common commands after hydrating the wrapper and dependencies:

```bash
./gradlew help
./gradlew projects
./gradlew validateDesignControlPack
./gradlew qualityGate
./gradlew printPublishedArtifacts
```

No command is expected to generate XML binding code in this pack.

Build conventions live in `build-logic/` as composable Gradle convention plugins. Published code modules live under `modules/`; non-published examples live under `examples/`.
