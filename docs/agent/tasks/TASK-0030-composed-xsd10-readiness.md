# TASK-0030: composed-xsd10-readiness

Status: accepted.

Task ID: `TASK-0030`
Gate: `0.3.0` Composed XSD 1.0 Schemas readiness; starts only after `TASK-0029` is accepted.
Requirement IDs: accepted `0.3.0` schema, binding, generation, validation, interop, Native Image, QA, and documentation IDs
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance, verification evidence, examples, release notes if present
Allowed files: requirements docs, conformance docs, verification docs, README/module/example docs, agent handoff/task cards, release docs
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.3.0` Composed XSD 1.0 vertical slice with honest conformance status for `XP-XSD10-COMPOSED`, including accepted group/attribute-group, list/union, and derivation flattening evidence, interop evidence, Native Image evidence, limitations, and next-slice draft readiness.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, round-trip, and Native Image evidence only
Documentation to update: all user-facing support/conformance docs affected by `0.3.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented native smoke or conformance command, `git diff --check`
Acceptance criteria: support claims match tested behavior; interop evidence is recorded; unsupported XSD 1.0 features remain explicit; next slice remains draft
Rollback notes: revert readiness-review docs and release metadata from this task

## Impact Notes

- Interop: readiness cannot pass without recorded interop evidence or an explained limitation.
- Native Image: representative composed-schema fixtures should run in selected lanes.
- Security: composition depth/cycle protections must be verified.
- Documentation: no full-XSD conformance claim unless matrix supports it.

## Readiness Checks

- Confirm support claims cover only the `TASK-0026` accepted `XP-XSD10-COMPOSED` subsets
  implemented by `TASK-0027`, `TASK-0028`, and `TASK-0029`.
- Confirm conformance rows for model groups, attribute groups, list/union simple types, and initial
  derivation match automated and documented interop evidence.
- Confirm unsupported group, list/union, and derivation shapes still produce explicit diagnostics.
- Confirm release docs still do not claim full XSD 1.0 conformance, artifact publication, or a
  release tag unless a separate release task authorizes it.

## Accepted Readiness Evidence

- Confirmed public support claims in README, module READMEs, compatibility profiles, conformance
  matrix, verification docs, release docs, requirements traceability, and handoff docs cover only
  accepted `TASK-0027`, `TASK-0028`, and `TASK-0029` `XP-XSD10-COMPOSED` behavior.
- Recorded the generated model surface for accepted composed schemas: flattened model group and
  attribute-group content, immutable `List<T>` required singleton list values, lexical `String`
  union values, and flattened complex extension fields with base fields before derived fields and no
  generated Java inheritance.
- Confirmed unsupported full XSD 1.0 features remain documented as out of scope, including repeated
  or optional list-valued XML fields, full derivation semantics, substitution groups, wildcards,
  mixed content, identity constraints, defaults/fixed semantics, and XSD 1.1.
- `git tag --list` returned no release tags; this task did not create a `v0.1.0`, `v0.2.0`,
  `v0.3.0`, or publication claim.

## Verification

- `./gradlew clean validateDesignControlPack qualityGate --console=plain` passed.
- `./gradlew :modules:generator-core:check :modules:conformance-tests:check --console=plain`
  passed.
- The scoped generator-core and conformance checks provide the documented `XP-XSD10-COMPOSED`
  conformance/interop evidence for positive and negative group, attribute-group, list/union, and
  derivation fixtures compared with JDK XML Schema validation where practical.
- `JAVA_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 GRAALVM_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 PATH=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2/lib/svm/bin:$PATH ./gradlew :modules:generator-core:generatedCodeNativeSmoke --console=plain`
  passed.
- `JAVA_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 GRAALVM_HOME=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2 PATH=/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2/lib/svm/bin:$PATH ./gradlew nativeSmoke --console=plain`
  was attempted. The generated-code native executable passed, then the aggregate failed in
  `:examples:multi-namespace:nativeTestCompile`, `:examples:purchase-order:nativeTestCompile`, and
  `:modules:runtime-core:nativeTestCompile` because the GraalVM Gradle plugin invoked the
  non-executable zero-byte
  `/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2/bin/native-image` instead of the
  executable
  `/home/jack/.gradle/jdks/graalvm_community-21-amd64-linux.2/lib/svm/bin/native-image`.
- `git diff --check` passed.
