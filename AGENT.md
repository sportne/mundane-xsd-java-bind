# Coding-agent governance

This file is a binding operating contract for coding agents working in this repository.

## Mandatory reading before any change

Read these files before editing:

1. `DESIGN_CONTROL_PACK_v0.1.md`
2. `docs/charter.md`
3. `docs/scope-and-non-goals.md`
4. `docs/architecture/overview.md`
5. `docs/architecture/module-boundaries.md`
6. `docs/requirements/index.md`
7. `docs/adr/index.md`
8. `docs/agent/handoff.md`

## Absolute rules

1. Do not implement XML schema parsing, binding, reading, writing, validation, or generated-code behavior until the design-control gate is accepted.
2. Every code or build change must cite requirement IDs.
3. Every behavioral change must add or update tests.
4. Runtime core and generated code must not depend on third-party libraries.
5. Generated binding behavior must not use reflection, annotation-driven discovery, ServiceLoader discovery, dynamic proxies, or classpath scanning without an approved ADR.
6. Runtime modules must remain Native Image friendly by default.
7. Do not weaken Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, JaCoCo, dependency verification, offline build, or Native Image policies without an ADR.
8. Do not silently broaden XML, namespace, XSD, XPath, JAXB, or validation scope.
9. Do not add network access in tests except explicitly tagged integration tests.
10. Do not commit generated product code except approved golden outputs and examples.
11. Do not modify golden outputs unless generator behavior intentionally changed and the review explains the diff.
12. Do not add dependencies without documenting module, scope, license, purpose, runtime impact, and Native Image impact.

## Required task-card format

```text
Task ID:
Requirement IDs:
ADR IDs:
Allowed files:
Forbidden files:
Expected behavior:
Tests to add/update:
Commands to run:
Acceptance criteria:
Rollback notes:
```

## Required pull-request checklist

```text
[ ] Requirements referenced
[ ] ADR impact reviewed
[ ] Module boundaries respected
[ ] No forbidden runtime dependency added
[ ] No runtime reflection/annotation binding added
[ ] Generated output deterministic where applicable
[ ] Unit tests added/updated where behavior changed
[ ] Golden tests added/updated where generated output changed
[ ] ArchUnit rules pass or are intentionally staged
[ ] Static analysis passes or staged exceptions are documented
[ ] Coverage thresholds pass or staged exceptions are documented
[ ] Native-image lane considered
[ ] Docs/conformance matrix updated if scope changed
```

## Escalation triggers

Open or update an ADR before proceeding if a task needs to:

- change standards baseline
- alter compatibility profiles
- add runtime-visible dependencies
- change model generation style
- add reflection or dynamic access
- change schema/resource resolution policy
- change validation phasing
- introduce a new XML parser abstraction
- change Native Image policy
- weaken any quality gate
