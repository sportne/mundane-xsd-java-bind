# Coding-agent governance

This file is the binding operating contract for coding agents working in this repository.

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
9. `docs/build/README.md`

## Absolute rules

1. Do not implement XML schema parsing, binding, reading, writing, validation, or generated-code behavior until the design-control gate is accepted.
2. Every code or build change must cite requirement IDs.
3. Every behavioral change must add or update tests.
4. Runtime core and generated code must not depend on third-party libraries.
5. Generated binding behavior must not use reflection, annotation-driven discovery, ServiceLoader discovery, dynamic proxies, or classpath scanning without an approved ADR.
6. Runtime modules must remain Native Image friendly by default.
7. Do not weaken Checkstyle, Spotless, SpotBugs, Error Prone, ArchUnit, JaCoCo, dependency verification, dependency locking, offline build, or Native Image policies without an ADR.
8. Do not silently broaden XML, namespace, XSD, XPath, JAXB, or validation scope.
9. Do not add network access in tests except explicitly tagged integration tests.
10. Do not commit generated product code except approved golden outputs and examples.
11. Do not modify golden outputs unless generator behavior intentionally changed and the review explains the diff.
12. Do not add dependencies without documenting module, scope, license, purpose, runtime impact, and Native Image impact.

## Change scope standard

Make the smallest coherent change that satisfies the approved task. Keep edits focused on the requested behavior, documentation, build rule, or policy change.

- Do not perform opportunistic refactors, renames, formatting sweeps, dependency upgrades, or documentation rewrites unless they are required to complete the task.
- Preserve existing requirements, engineering plans, verification plans, validation plans, ADRs, and traceability records unless the task explicitly changes them.
- When a cleanup is useful but not necessary, document it as follow-up work instead of mixing it into the current change.

## Test-driven implementation standard

For implementation and defect-fix tasks, prefer a test-first loop:

- Add or update one focused behavior test that fails for the missing behavior or defect.
- Implement the smallest production change that makes that test pass.
- Repeat in vertical slices instead of writing a broad batch of speculative tests.
- If test-first work is impractical for a change, explain why in the review notes and still add the closest useful regression coverage before completion.

## Build-file documentation standard

Gradle files are contributor-facing documentation, not just machine instructions.

When editing `settings.gradle`, `build.gradle`, `gradle.properties`, `build-logic/**/*.gradle`, or module/example `build.gradle` files:

- Write comments for a recent high school graduate who is comfortable with computers but new to Gradle.
- Explain what each plugin, property, task, dependency block, publication block, repository choice, and non-obvious Gradle API call is doing.
- Prefer short comments next to the thing being explained.
- Explain why a project-specific convention exists, especially for offline builds, dependency verification, dependency locking, Java toolchains, coverage, publishing, and Native Image.
- Do not remove explanatory comments just because the Gradle code feels obvious to an experienced build engineer.

## Documentation consistency standard

Documentation is part of the project contract. When editing docs, build files, module layout, workflows, tools, or agent policy:

- Keep `README.md`, `CONTRIBUTING.md`, `docs/build/README.md`, `docs/infrastructure/*.md`, module READMEs, and task handoff docs consistent with the actual repository layout.
- Verify task names, Gradle properties, module names, paths, and workflow names against the files that define them before documenting them.
- Update indexes when moving or replacing documentation.
- Prefer one canonical document for each topic; remove obsolete compatibility documents while the project is pre-1.0 unless a task explicitly requires a transition file.
- Run `./gradlew validateDesignControlPack qualityGate` and `git diff --check` after documentation changes that affect build, layout, or agent governance.

## Pre-1.0 compatibility policy

This project has not reached version 1.0, so agents do not need to preserve backwards compatibility when a breaking change makes the design, build, API, module layout, documentation, or contributor workflow clearer.

- Prefer the cleaner long-term shape over compatibility shims, duplicate entry points, or legacy aliases while the project is pre-1.0.
- If a pre-1.0 breaking change removes or renames a public task, property, module, package, artifact, file path, or documented workflow, update every affected document and example in the same change.
- After version 1.0, breaking changes must be reserved for the next major version and documented through the normal requirement and ADR process.

## Required task shape

Use `docs/agent/implementation-task-template.md` for implementation tasks.

## Task status and handoff protocol

Before promoting or executing a task:

- Review `docs/agent/handoff.md`, the target task card, `docs/roadmap.md`, `docs/requirements/traceability-matrix.md`, and `docs/conformance/matrix.md`.
- Confirm the task status, next-task ordering, allowed files, required evidence, and stated scope are still correct.
- Look for stale statuses, stale completion notes, and documentation that overclaims implemented support.

When a task is executed:

- Promote the task card and handoff before implementation starts.
- Mark the task accepted/completed only after the task's required verification has passed.
- Record meaningful command evidence in the task card or handoff when the task changes behavior, gates, readiness status, or public documentation.
- Advance the handoff to the next draft gate after completion.

## Review-before-commit standard

Before suggesting a commit message or asking for review:

- Inspect `git diff` and review the change as if it came from another contributor.
- Check for accidental scope expansion, stale documentation, missing tests, missing evidence, overbroad generated diffs, and quality-gate policy drift.
- Address actionable findings before presenting the change as ready.
- Run `git diff --check` before final handoff.

## Historical documentation standard

Historical scaffold, readiness, or verification documents may remain in the
repository when they are useful evidence, but they must not read like current
state if the project has moved on.

- Label historical snapshots clearly.
- Update or qualify scaffold-era statements after product implementation exists.
- Do not leave obsolete "not started", "placeholder only", or "no implementation" wording in contributor-facing docs unless the text is explicitly historical.

## Native Image evidence standard

Native Image compatibility is a project requirement, not an optional polish item.

- Run the documented Native Image checks when a task requires native evidence.
- If `native-image` is not available on `PATH` or through `JAVA_HOME`, look for SDK-managed Java installations before declaring the lane unavailable.
- Keep Native Image out of default local `qualityGate` unless a task or ADR changes that policy.
- Do not mark native-related tasks complete until native command evidence is recorded or a concrete blocker is documented.

## Architecture-rule upkeep

Architecture rules are part of the implementation contract.

- When adding production code, consider whether existing ArchUnit rules cover the new package or module.
- If a new production surface creates a new architectural boundary, add or update ArchUnit coverage and the architecture rule catalog in the same task when in scope.
- If a rule-worthy concern is out of scope, record it as follow-up work rather than relying on memory.

## Support-claim discipline

Public documentation must distinguish implemented support from planned support.

- README, compatibility profiles, conformance docs, release docs, examples, and module READMEs must not imply support for XSD features that are still draft or deferred.
- Use explicit future/deferred language for unsupported schema features, validation semantics, runtime behavior, and release capabilities.
- When implementation scope changes, update user-facing support claims in the same change.

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
[ ] ArchUnit rules pass or staged exceptions are documented
[ ] Static analysis passes or staged exceptions are documented
[ ] Coverage thresholds pass or staged exceptions are documented
[ ] Native-image lane considered
[ ] Docs/conformance matrix updated if scope changed
[ ] Task status and handoff updated if task state changed
[ ] Command evidence recorded when required
[ ] Diff reviewed before commit message or review request
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
