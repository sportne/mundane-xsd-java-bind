# TASK-0073: xml-security-posture-review

Status: draft.

Task ID: `TASK-0073`
Priority: P1
Gate: XML security posture review.
Target areas: schema resolver, syntax parser XML factory use, runtime-jdkxml adapters, generated XML
validators/readers, wildcard fragment handling, conformance harness XML parsing, security docs, and
tests.
Allowed files: runtime/generator/conformance tests, small malicious XML/XSD fixtures, docs, and
narrow production fixes for confirmed security defects.
Forbidden files: broad parser rewrites, DOM-backed binding, DTD/entity identity preservation, network
resource enablement by default, release metadata, dependencies without ADR, or quality-gate
weakening.
Expected behavior: threat-model every XML parsing path and prove secure defaults for DTD denial,
external entity denial, schema import/include resolution policy, resolver diagnostics, retained
wildcard fragments, and generated XML validation. Compare against known ecosystem failure modes such
as XXE from parser defaults.
Tests to add/update: XXE/entity expansion/resource access denial tests for each parser path, local
file disclosure attempts, network-denied resolver fixtures, and Native Image security path coverage
where practical.
Commands to run: impacted module checks, `./gradlew validateDesignControlPack qualityGate --console=plain`,
SDKMAN GraalVM native lanes if native security paths change, and `git diff --check`.
Acceptance criteria: every XML parse path has documented secure defaults and tests; any intentional
unsupported behavior is explicit; no default path can fetch network resources or resolve external
entities.
Rollback notes: revert security tests/docs/fixes and re-open a blocker task if a defect cannot be
fixed narrowly.

