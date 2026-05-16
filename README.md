# xsd-bind-java

**Status:** Design-Control Pack v0.1 scaffold. No XML schema compiler, runtime binding, reader, writer, validator, or generated-code implementation exists yet.

`xsd-bind-java` is a schema-to-code generator and runtime architecture for Java. It is conceptually adjacent to JAXB/Jakarta XML Binding, but it is deliberately designed as a modern, explicit, generated-code system with strong engineering controls and GraalVM Native Image friendliness from the beginning.

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
- `docs/requirements/` — requirements taxonomy and phase-one requirements.
- `docs/architecture/` — architecture and module boundaries.
- `docs/verification/` — verification and validation strategy.
- `docs/adr/` — initial architectural decisions.
- `docs/agent/handoff.md` — next-step task sequence for coding agents.

## Build note

This scaffold is configured for Gradle 9.5.1 with Groovy DSL. The checked-in `gradlew` script bootstraps `gradle/wrapper/gradle-wrapper.jar` on first use from Gradle's distribution service, then delegates to the standard Gradle Wrapper main class. For fully offline builds, hydrate both the wrapper JAR and the local Maven repository as described in `docs/infrastructure/offline-build-plan.md`.

Common commands after hydrating the wrapper and dependencies:

```bash
./gradlew help
./gradlew projects
./gradlew designControlStatus
./gradlew check
```

No command is expected to generate XML binding code in this pack.
