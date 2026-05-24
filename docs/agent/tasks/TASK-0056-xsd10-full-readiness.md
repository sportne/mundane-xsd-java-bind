# TASK-0056: xsd10-full-readiness

Status: accepted.

Task ID: `TASK-0056`
Gate: final full XSD 1.0 readiness review.
Target areas: README, compatibility docs, conformance matrix, verification plan, release/readiness docs, traceability, handoff
Allowed files: docs, test evidence records, task handoff
Forbidden files: product behavior, dependency changes, release tags, publication, signing, XSD 1.1/XML 1.1
Expected behavior: reconcile full XSD 1.0 implementation and conformance evidence before any public full-support claim is made.
Tests to add/update: documentation consistency checks only unless gaps are discovered.
Acceptance criteria: `XP-XSD10-FULL` is claimed only if feature matrix, conformance suite, generated-code smoke, quality gate, and optional native lanes agree with evidence.
Rollback notes: revert readiness docs and support-claim changes from this task.

## Completion notes

- Reconciled public and contributor documentation after `TASK-0055` W3C suite intake. The docs now
  distinguish selected executable profile support, W3C classification evidence, and remaining
  blockers to full XSD 1.0 generated-binding support.
- Kept `GeneratorProfile.XP_XSD10_FULL` documented as a planned public token that generation
  intentionally rejects. `XP-XSD10-FULL` is not advertised as executable.
- Recorded the readiness decision as negative for full-support claims: grouped content-list models,
  complete UPA coverage, complete derivation/restriction/block/final and `xsi:type` behavior,
  strict/lax schema-known wildcard validation, wildcard choice branches, and W3C rows mapped to
  generated-binding support remain future work.
- Updated handoff to state that no implementation gate is currently open; any new full-XSD
  gap-closure work requires a new accepted task sequence.

## Verification evidence

Baseline:

```text
git status --short --branch
## main...origin/main
 M README.md
 M docs/agent/handoff.md
 M docs/agent/tasks/TASK-0056-xsd10-full-readiness.md
 M docs/architecture/validation-architecture.md
 M docs/compatibility-profiles.md
 M docs/conformance/matrix.md
 M docs/conformance/w3c-test-suite-policy.md
 M docs/infrastructure/release-notes-0.6.0-alpha.md
 M docs/infrastructure/release-plan.md
 M docs/requirements/traceability-matrix.md
 M docs/roadmap.md
 M docs/verification/conformance-strategy.md
 M docs/verification/verification-plan.md
 M docs/verification/xsd10-full-feature-matrix.md
 M modules/conformance-tests/README.md
command -v native-image || true
<no output>
```

Commands:

```text
./gradlew :modules:conformance-tests:check :modules:generator-core:generatedCodeSmoke --console=plain
BUILD SUCCESSFUL in 8s

./gradlew -Pmxjb.w3cXsd10SuiteDir=/mnt/d/projects/mundane-xsd-java-bind/build/tmp/w3c-xsd/extract/xmlschema2006-11-06 w3cXsd10Conformance --console=plain
BUILD SUCCESSFUL in 31s

modules/conformance-tests/build/reports/w3c-xsd10-conformance/summary.txt
w3c-xsd10-summary total=24796 binding-supported=0 validation-only=24439 tolerated-metadata=98 expected-diagnostic=2 product-scope-incompatible=167 blocked=90

./gradlew validateDesignControlPack qualityGate --console=plain
BUILD SUCCESSFUL in 26s

./gradlew :modules:conformance-tests:checkNativeConformanceToolchain --console=plain
FAILED as expected because local native-image is unavailable:
native-image was not found. Run this task with GraalVM native-image on PATH or set JAVA_HOME to a GraalVM installation that includes native-image.
```
