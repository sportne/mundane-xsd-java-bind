# TASK-0002: hydrate-gradle-wrapper-and-dependencies

Status: completed for the current scaffold; repeat whenever plugins, dependencies, wrapper metadata, verification metadata, or lockfiles change.

Requirement IDs: `REQ-BUILD-001`, `REQ-BUILD-002`, `REQ-BUILD-003`
ADR IDs: `ADR-0011`, `ADR-0012`
Allowed files: `gradle/wrapper/**`, `gradle/verification-metadata.xml`, dependency lock files, offline repo docs
Forbidden files: product implementation source
Expected behavior: wrapper metadata, dependency verification metadata, and dependency locks are hydrated and verified.
Tests to add/update: none; build sanity only.
Commands to run: `./gradlew help`, `./gradlew projects`, `./gradlew --write-verification-metadata sha256 help`
Acceptance criteria: Gradle wrapper runs online, dependency verification metadata and locks are hydrated/reviewed, and the documented strict verification path is valid.
Rollback notes: remove generated lock/metadata changes if invalid.
