# TASK-0035: xsd10-semantic-expansion-readiness

Status: accepted.

Task ID: `TASK-0035`
Gate: `0.4.0` XSD 1.0 Semantic Expansion readiness; starts only after `TASK-0034` is accepted.
Requirement IDs: accepted `0.4.0` schema, binding, model, validation, generation, interop, Native Image, QA, and documentation IDs
ADR IDs: `ADR-0001` through `ADR-0014`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/verification-plan.md`, `docs/infrastructure/release-plan.md`
Target areas: documentation, conformance, verification evidence, examples, release notes if present
Allowed files: requirements docs, conformance docs, verification docs, README/module/example docs, agent handoff/task cards, release docs
Forbidden files: new product behavior, unsupported schema expansion, dependency metadata, quality-gate weakening
Expected behavior: verify and document the `0.4.0` semantic expansion vertical slice for `XP-XSD10-SEMANTIC` with conformance status, interop evidence, Native Image evidence, limitations, and next-slice draft readiness.
Tests to add/update: documentation command checks where available; final quality, conformance, interop, round-trip, and Native Image evidence only
Documentation to update: all user-facing support/conformance docs affected by `0.4.0`
Commands to run: `./gradlew clean validateDesignControlPack qualityGate`, documented interop/conformance commands, documented native command, `git diff --check`
Acceptance criteria: semantic support claims match test evidence; interop evidence is recorded; document-oriented features remain future
Rollback notes: revert readiness-review docs and release metadata from this task

## Impact Notes

- Interop: readiness requires recorded evidence or explicit limitation notes.
- Native Image: semantic fixtures should run in selected lanes.
- Security: semantic diagnostics remain safe and deterministic.
- Documentation: no wildcard/mixed-content claims.

## Readiness Checks

- Confirm support claims cover only accepted `TASK-0032`, `TASK-0033`, and `TASK-0034`
  `XP-XSD10-SEMANTIC` behavior.
- Confirm conformance rows for nillable/default/fixed semantics, direct substitution groups, and
  semantic validation match automated and documented interop evidence.
- Confirm unsupported semantic, substitution, identity-constraint, wildcard, mixed-content,
  full-datatype, full-derivation, and XSD 1.1 shapes still produce explicit diagnostics.
- Confirm release docs still do not claim full XSD 1.0 conformance, artifact publication, or a
  release tag unless a separate release task authorizes it.

## Accepted Readiness Evidence

- Confirmed public support claims in README, module READMEs, compatibility profiles, conformance
  matrix, verification docs, release docs, requirements traceability, and handoff docs cover only
  accepted `TASK-0032`, `TASK-0033`, and `TASK-0034` `XP-XSD10-SEMANTIC` behavior.
- Recorded the generated model surface for accepted semantic schemas: required singleton nillable
  elements bind as `Optional<T>`, scalar default/fixed values are read as effective values where
  accepted, direct substitution-group head references use sealed branch models, and generated
  validators cover accepted nil/default/fixed and substitution validation paths.
- Confirmed unsupported full XSD 1.0 features remain documented as out of scope, including full
  substitution groups, identity constraints, wildcards, mixed content, full datatype semantics, full
  derivation semantics, and XSD 1.1.
- `git tag --list` returned no release tags; this task did not create a `v0.1.0`, `v0.2.0`,
  `v0.3.0`, `v0.4.0`, or publication claim.

## Verification

- `./gradlew clean validateDesignControlPack qualityGate --console=plain` passed.
- `./gradlew :modules:generator-core:check :modules:conformance-tests:check --console=plain`
  passed.
- The scoped generator-core and conformance checks provide the documented `XP-XSD10-SEMANTIC`
  conformance/interop evidence for positive and negative nillable/default/fixed, direct
  substitution-group, and semantic validation fixtures compared with JDK XML Schema validation
  where practical.
- `JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.2-graalce PATH=$HOME/.sdkman/candidates/java/21.0.2-graalce/bin:$PATH ./gradlew nativeSmoke --console=plain`
  passed. The aggregate built and ran the generated-code native smoke executable, runtime-core
  native tests, runtime-jdkxml native tests, and purchase-order and multi-namespace example native
  tests.
- Documentation search confirmed no stale claim remains that `TASK-0034` or accepted semantic
  validation hardening is still planned.
- `git diff --check` passed.
- Semantic conformance/interop: `T-CONF-XP-XSD10-SEMANTIC-*` and `T-INTEROP-SEMANTIC-*` fixtures
  include positive and negative cases compared against JDK XML Schema validation where practical.
- Native Image: representative semantic generated-code paths for nillable/default/fixed, direct
  substitution groups, and generated semantic validation join the smoke lane as readiness evidence;
  broader Native Image conformance remains a later `TASK-0044` concern.
