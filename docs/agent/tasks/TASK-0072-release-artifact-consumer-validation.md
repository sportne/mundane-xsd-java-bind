# TASK-0072: release-artifact-consumer-validation

Status: draft.

Task ID: `TASK-0072`
Priority: P1
Gate: downstream release-asset consumption validation.
Target areas: release asset instructions, build docs, optional consumer-smoke fixture, GitHub Release
asset layout, and verification docs.
Allowed files: docs, scripts or tests that create temporary consumer projects, and release asset
validation tasks that use local or downloaded GitHub Release assets.
Forbidden files: Maven Central/package-registry publishing, signing, release retagging, product
behavior expansion, dependencies without ADR, or quality-gate weakening.
Expected behavior: prove a clean downstream Java project can consume the GitHub Release Maven-layout
zip without relying on the source checkout. Exercise at least one runtime dependency path, CLI or
Gradle plugin path, generated source compilation, generated read/write/validate, and clear failure
diagnostics when the local repository path is wrong.
Tests to add/update: consumer-smoke test or script with deterministic temporary directories; docs
validation for release-asset consumption instructions.
Commands to run: the new consumer-smoke command, `./gradlew validateDesignControlPack qualityGate --console=plain`,
and `git diff --check`.
Acceptance criteria: release assets are demonstrably usable by a downstream project; README/build
docs explain the supported consumption path; no remote publication or signing is introduced.
Rollback notes: remove consumer-smoke harness and docs.

