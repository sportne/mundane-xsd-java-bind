# TASK-0045: release-engineering-publication-readiness

Status: accepted.

Task ID: `TASK-0045`
Gate: `0.6.0` Hardening and Release Maturity; starts only after `TASK-0044` is accepted.
Requirement IDs: future `REQ-REL-*`, accepted build, QA, docs, conformance, Native Image, and interop IDs
ADR IDs: `ADR-0011`, `ADR-0012`, `ADR-0013`, plus release-policy ADRs if added
Specification references: `docs/infrastructure/release-plan.md`, `docs/build/README.md`, `docs/infrastructure/ci-plan.md`, `docs/verification/verification-plan.md`
Target areas: release docs, publishing configuration, CI, artifact metadata, examples, README
Allowed files: publishing/release docs, artifact metadata, CI release workflows if approved, build scripts only for publication behavior, README/module docs, traceability docs
Forbidden files: product feature implementation, dependency updates without review, quality-gate weakening, conformance overclaims
Expected behavior: prepare artifact publication and release workflow for public alpha/beta maturity, including coordinates, signing/staging policy if needed, release notes, supported profile statement, conformance evidence links, and rollback instructions.
Tests to add/update: publication dry-run or local publish validation, docs command checks, artifact metadata checks, release workflow dry-run where practical, secret/cache exclusion checks where practical
Documentation to update: release plan, build docs, README, module READMEs, verification/conformance docs, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, documented publication dry-run command, `git diff --check`
Acceptance criteria: release process is reproducible in dry-run/local validation; artifacts identify supported profiles and conformance status; release notes name unsupported features; no release claim lacks tests/docs; no actual publication or release tag occurs unless explicitly authorized
Rollback notes: revert release workflow/build/docs changes from this task

## Accepted Planning Scope

- Validate planned artifact metadata, coordinates, signing/staging policy, and rollback steps.
- Add only dry-run or local publication checks unless a maintainer explicitly authorizes real
  publication.
- Public support statements must link to conformance, benchmark, Native Image, and security
  evidence from prior `0.6.0` tasks.
- Release artifacts and docs must exclude local caches, secrets, generated temporary files, and
  unsupported conformance claims.

## Impact Notes

- Interop: release notes must reference available interop evidence for supported profiles.
- Native Image: release readiness must include native conformance status.
- Security: release artifacts must not include local `.repo` caches or secrets.
- Documentation: public support statements must match conformance matrix.

## Completion Notes

- Added root `publicationDryRun`, `stagePublications`, `cleanPublicationStaging`, and
  `validatePublicationStaging` lanes. The dry-run stages approved Maven artifacts to
  `build/staging-repository` and validates expected coordinates, required files, staged metadata,
  plugin marker publication, release-note non-claims, and local path/secret-like leakage.
- Kept `gradle.properties` at `0.1.0-SNAPSHOT`; candidate dry-run validation uses
  `-Pmxjb.version=0.6.0-alpha.0`.
- Normalized Gradle plugin publication so the implementation artifact stages as
  `io.github.mundanej:mxjb-gradle-plugin` plus the marker
  `io.github.mundanej.mxjb:io.github.mundanej.mxjb.gradle.plugin`; the unintended
  `io.github.mundanej:generator-gradle-plugin` coordinate is not staged.
- Extended staged POM metadata with project URL, Apache 2.0 license, SCM coordinates, and
  maintainer identity without adding signing, secrets, remote repositories, release tags, or
  publication workflows.
- Added `docs/infrastructure/release-notes-0.6.0-alpha.md` with supported profiles, selected
  conformance/benchmark/Native Image evidence, rollback steps, and explicit non-claims for remote
  publication, release tags, signing, full XSD 1.0 conformance, XSD 1.1, XML Canonicalization,
  XML Signature canonical forms, lexical prefix preservation, comments/PIs, DTD/entity identity,
  hard performance guarantees, and unavailable Native Image tooling.
- Updated README, build docs, release plan, CI plan, conformance matrix, verification plan,
  traceability matrix, BOM notes, and handoff for the accepted release-engineering dry-run gate.

## Verification Evidence

- `git tag --list` returned no release tags before this task's dry-run evidence.
- `./gradlew printPublishedArtifacts --console=plain` passed and printed the approved BOM,
  runtime, generator, Gradle plugin, and testing-support coordinates.
- `./gradlew -Pmxjb.version=0.6.0-alpha.0 publicationDryRun --console=plain` passed and reported
  `Validated 9 staged publication coordinates for 0.6.0-alpha.0.`
- `./gradlew validateDesignControlPack qualityGate --console=plain` passed.
- `git diff --check` passed.
