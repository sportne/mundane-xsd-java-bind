# TASK-0046: hardening-release-maturity-readiness

Status: draft.

Task ID: `TASK-0046`
Gate: `0.6.0` Hardening and Release Maturity readiness; starts only after `TASK-0045` is accepted.
Requirement IDs: accepted `0.6.0` conformance, interop, performance, Native Image, release, QA, build, docs, and agent IDs
ADR IDs: `ADR-0001` through `ADR-0014`, plus any release or future-profile ADRs accepted by this point
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance evidence, release evidence, verification records, agent handoff
Allowed files: requirements docs, conformance docs, verification docs, release docs, README/module/example docs, agent handoff/task cards, changelog/release notes if present
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.6.0` hardening and release maturity vertical slice, including interop/conformance depth, performance baselines, Native Image conformance, release readiness, limitations, security posture, and future profile recommendations.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, benchmark, Native Image, publication dry-run, and security evidence only
Documentation to update: all user-facing and contributor-facing docs affected by `0.6.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented benchmark command, documented native conformance command, documented publication dry-run, `git diff --check`
Acceptance criteria: `0.6.0` maturity claims match evidence; release process is documented and dry-run evidence exists; interop remains an ongoing verification lane; benchmark claims remain advisory unless proven stable; future XSD 1.1 work remains gated by ADR
Rollback notes: revert readiness-review docs and release metadata from this task

## Accepted Planning Scope

- Reconcile evidence from `TASK-0042` through `TASK-0045` across README, compatibility profiles,
  conformance matrix, verification docs, release docs, traceability, and handoff.
- Confirm unsupported feature lists still exclude full XSD 1.0, XSD 1.1, XML Canonicalization,
  unimplemented wildcard/mixed variants, identity constraints, and unproven performance claims.
- Confirm no release tag or real artifact publication is claimed unless separately authorized.
- Advance handoff only after final quality, conformance/interop, benchmark, Native Image, release
  dry-run, and whitespace evidence is recorded or concrete toolchain blockers are documented.

## Impact Notes

- Interop: summarize recurring interop evidence from all prior post-0.1.0 slices.
- Native Image: summarize native conformance scope and gaps.
- Security: confirm release and test artifacts exclude local caches/secrets.
- Documentation: make future-profile recommendations without authorizing them.
