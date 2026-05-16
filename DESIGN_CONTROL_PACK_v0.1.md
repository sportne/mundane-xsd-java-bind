# Design-Control Pack v0.1

This pack establishes the repository, design baseline, requirements taxonomy, architecture, verification strategy, infrastructure plan, ADRs, and agent governance for `xsd-bind-java`.

## Pack status

- Pack version: `0.1`
- Product implementation status: **not started**
- Permitted content: documentation, governance, Gradle/build infrastructure scaffold, CI skeleton, placeholder module directories, examples placeholders
- Forbidden content in this pack: schema compiler implementation, runtime XML reader/writer implementation, generated binding implementation, validation engine implementation

## Acceptance checklist

```text
[ ] Repository tree exists.
[ ] No XML product implementation exists.
[ ] All planned modules are declared.
[ ] All design documents exist with initial content.
[ ] Requirement taxonomy exists.
[ ] Phase-one requirements are drafted.
[ ] ADR index and initial ADRs exist.
[ ] Agent rules exist in AGENT.md.
[ ] Build uses Gradle Groovy DSL.
[ ] Java toolchain baseline is Java 21.
[ ] Java 25 compatibility lane is documented.
[ ] Quality gate configuration files exist.
[ ] JaCoCo aggregate/per-file policy is documented.
[ ] Native Image test plan exists.
[ ] Offline build plan exists.
[ ] Conformance strategy references W3C XML/XSD suites.
[ ] ./gradlew projects succeeds after wrapper hydration.
[ ] ./gradlew help succeeds after wrapper hydration.
[ ] CI workflows are present but do not fake product tests.
```

## Handoff rule

A coding agent may proceed only with tasks listed in `docs/agent/handoff.md`, and only within the files allowed by that task card. Any change to scope, profiles, runtime dependency policy, reflection policy, validation phasing, or Native Image posture requires an ADR update before implementation.

## Initial quality posture

The build scaffold declares Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, JaCoCo, and Native Image support. Some gates will become meaningful only after source code exists, but the policies and wiring are intentionally present at project start.
