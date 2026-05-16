# TASK-0045: release-engineering-publication-readiness

Status: draft.

Task ID: `TASK-0045`
Gate: `0.6.0` Hardening and Release Maturity; starts only after `TASK-0044` is accepted.
Requirement IDs: future `REQ-REL-*`, accepted build, QA, docs, conformance, Native Image, and interop IDs
ADR IDs: `ADR-0011`, `ADR-0012`, `ADR-0013`, plus release-policy ADRs if added
Specification references: `docs/infrastructure/release-plan.md`, `docs/build/README.md`, `docs/infrastructure/ci-plan.md`, `docs/verification/verification-plan.md`
Target areas: release docs, publishing configuration, CI, artifact metadata, examples, README
Allowed files: publishing/release docs, artifact metadata, CI release workflows if approved, build scripts only for publication behavior, README/module docs, traceability docs
Forbidden files: product feature implementation, dependency updates without review, quality-gate weakening, conformance overclaims
Expected behavior: prepare artifact publication and release workflow for public alpha/beta maturity, including coordinates, signing/staging policy if needed, release notes, supported profile statement, conformance evidence links, and rollback instructions.
Tests to add/update: publication dry-run or local publish validation, docs command checks, artifact metadata checks, release workflow dry-run where practical
Documentation to update: release plan, build docs, README, module READMEs, verification/conformance docs, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, documented publication dry-run command, `git diff --check`
Acceptance criteria: release process is reproducible; artifacts identify supported profiles and conformance status; no release claim lacks tests/docs
Rollback notes: revert release workflow/build/docs changes from this task

## Impact Notes

- Interop: release notes must reference available interop evidence for supported profiles.
- Native Image: release readiness must include native conformance status.
- Security: release artifacts must not include local `.repo` caches or secrets.
- Documentation: public support statements must match conformance matrix.
