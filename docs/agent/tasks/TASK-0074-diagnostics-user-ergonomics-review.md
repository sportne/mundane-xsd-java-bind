# TASK-0074: diagnostics-user-ergonomics-review

Status: draft.

Task ID: `TASK-0074`
Priority: P2
Gate: diagnostics and user ergonomics review.
Target areas: generator diagnostics, CLI output, Gradle plugin failures, W3C/selected conformance
reports, README/build docs, and tests that assert stable diagnostics.
Allowed files: docs, diagnostic tests, CLI/Gradle output tests, and narrow message/category
improvements that do not change behavior.
Forbidden files: schema-feature expansion, dependencies, release metadata, or quality-gate weakening.
Expected behavior: review the most common failure modes a user will hit: unsupported schema shapes,
namespace/package configuration mistakes, missing schema files, resolver denials, invalid XML,
invalid object validation, W3C suite path mistakes, and release-asset consumption mistakes. Improve
messages so they include stable code/category, location when available, and a useful next action.
Tests to add/update: exact diagnostic tests for public API, CLI, Gradle plugin, and conformance
harness output.
Commands to run: impacted module checks, `./gradlew validateDesignControlPack qualityGate --console=plain`,
and `git diff --check`.
Acceptance criteria: public failures are actionable without reading source code; stable diagnostic
codes are preserved or migrated deliberately; no user-facing output leaks local absolute paths unless
the path was user-provided and necessary.
Rollback notes: revert diagnostic and docs changes.

