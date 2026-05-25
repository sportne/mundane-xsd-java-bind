# TASK-0068: external-issue-regression-mining

Status: draft.

Task ID: `TASK-0068`
Priority: P0
Gate: external failure-mode mining and regression-plan intake.
Target areas: issue trackers and release notes for functionally similar XML Schema binding/codegen
projects, selected local fixture manifest, conformance strategy, task handoff, and a new issue-mining
evidence note if needed.
Allowed files: docs, selected conformance fixture metadata, small local regression fixtures, and
tests that reproduce already-supported behavior or stable unsupported diagnostics.
Forbidden files: broad external suite vendoring, product behavior expansion, dependencies, release
metadata, Maven Central publishing, signing, XSD 1.1/XML 1.1 support, canonical XML, DOM-backed
binding, or quality-gate weakening.
Expected behavior: mine recurring issues from projects such as JAXB/XJC, jaxb-tools, xsdata,
XmlSchemaClassGenerator, xscgen, and other schema-to-code tools. Classify each issue theme as
already covered, needs local regression fixture, documented non-goal, or future implementation task.
Initial themes must include choice ordering/list shape, wildcard ambiguity, substitution and
abstract dispatch, defaults/nullability/nil, security/XXE, naming collisions, setup/tooling
confusion, generated API ergonomics, and AOT/Native Image behavior.
Tests to add/update: add only small regression fixtures for behavior already claimed by this project
or deterministic diagnostics for out-of-scope constructs; do not expand support under this task.
Commands to run: `./gradlew :modules:generator-core:check :modules:conformance-tests:check --console=plain`
when fixtures/tests change; always run `./gradlew validateDesignControlPack qualityGate --console=plain`
and `git diff --check`.
Acceptance criteria: every mined issue theme has a disposition and source link; high-risk themes
that map to current support have at least one local fixture or a clear follow-on task; no issue is
used to silently broaden scope.
Rollback notes: remove the issue-mining note, fixture additions, and handoff updates.

