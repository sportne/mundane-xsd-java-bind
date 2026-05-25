# TASK-0067: claims-evidence-reconciliation

Status: accepted.

Task ID: `TASK-0067`
Priority: P0
Gate: post-1.0.0 truth-in-claims review.
Target areas: README, compatibility profiles, conformance matrix, XSD 1.0 feature matrix,
verification plan, release notes, task handoff, and any public docs that describe full XSD 1.0
support.
Allowed files: docs, conformance metadata reports, task cards, and tests that only enforce claim
wording or matrix consistency.
Forbidden files: product behavior, release tags, publication changes, dependency changes, Maven
Central publishing, signing, XSD 1.1, XML 1.1, XML Canonicalization, lexical prefix preservation,
DTD/entity identity, DOM-backed binding, or quality-gate weakening.
Expected behavior: reconcile every public `1.0.0`, `XP-XSD10-FULL`, and "full/practical XSD 1.0"
claim with the actual conformance matrix and feature matrix evidence. Where evidence is narrower
than wording, either narrow the wording or open a later task that supplies missing evidence before
the claim is retained.
Tests to add/update: docs validation for inconsistent support terms where practical; no product
tests unless a doc claim directly contradicts an executable fixture.
Commands to run: `./gradlew validateDesignControlPack qualityGate --console=plain`,
`git diff --check`.
Acceptance criteria: no public doc overstates W3C full-suite generated-binding coverage; partially
verified areas are described with their exact evidence limits; `1.0.0` release wording remains
accurate but not broader than demonstrated behavior; non-goals remain explicit.
Rollback notes: revert documentation and claim-validation changes.

## Completion notes

`TASK-0067` reconciles public `1.0.0`, `XP-XSD10-FULL`, and full-XSD wording against the current
feature and conformance evidence. The accepted wording now describes `XP-XSD10-FULL` as executable
for the project's accepted generated-binding product scope, not as broad W3C full-suite generated
binding coverage.

Updated docs include README, compatibility profiles, roadmap, conformance and verification plans,
module README wording, release notes, and `docs/verification/support-claims-reconciliation.md`.

Verification:

- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
