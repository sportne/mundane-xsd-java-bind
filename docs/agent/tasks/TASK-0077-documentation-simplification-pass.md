# TASK-0077: documentation-simplification-pass

Status: draft.

Task ID: `TASK-0077`
Priority: P3
Gate: post-1.0.0 documentation simplification.
Target areas: README, getting-started docs, build docs, release-consumption docs, compatibility
profiles, generated-code contract links, and task/evidence archive organization.
Allowed files: docs and docs-validation tests.
Forbidden files: product behavior, release metadata changes, dependency changes, support-claim
expansion, or quality-gate weakening.
Expected behavior: make user-facing docs short, current, and task-oriented while preserving detailed
evidence in deeper docs. Separate "how to use the released tool" from "how the project proved it";
make the first successful CLI/Gradle path easy to follow; keep non-goals visible without overwhelming
the quick start.
Tests to add/update: docs validation for moved links and required current commands.
Commands to run: `./gradlew validateDesignControlPack qualityGate --console=plain`,
`git diff --check`.
Acceptance criteria: README and getting-started docs can be read without understanding the task
history; detailed evidence remains discoverable; no support claim is broadened.
Rollback notes: revert docs simplification changes.

