# October 2026 maintenance

Task ID: MAINT-2026-10
Gate: Maintainer-requested build infrastructure maintenance
Requirement IDs: REQ-BUILD-002, REQ-BUILD-003, REQ-QA-001, REQ-NI-001
ADR IDs: ADR-0011, ADR-0012
Specification references: Existing build and verification contracts; no schema scope change.
Target module: GitHub Actions workflows only.
Allowed files: .github/workflows/**, docs/agent/maintenance-2026-10.md, docs/agent/handoff.md
Forbidden files: Product implementation, generated output, dependency locks, standards and profiles.
Expected behavior: Refresh deprecated action runtimes while retaining every existing JVM, native and release gate.
Tests to add/update: No product behavior changes; existing gates remain authoritative.
Documentation to update: This scoped maintenance record and handoff reference.
Commands to run: ./gradlew validateDesignControlPack qualityGate --console=plain; git diff --check
Acceptance criteria: Workflow changes preserve commands, matrix coverage and permission scope. Record local and hosted verification in the pull request.
Rollback notes: Revert workflow and maintenance-document changes together.

This maintenance is authorized by the repository owner's October 17 request.
It neither advances nor accepts an unrelated product roadmap task. Native Image
execution requires the existing GraalVM CI lane; local JVM evidence alone is not native evidence.
