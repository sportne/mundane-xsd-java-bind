# TASK-0032: nillable-default-fixed-semantics

Status: draft.

Task ID: `TASK-0032`
Gate: `0.4.0` XSD 1.0 Semantic Expansion; starts only after `TASK-0031` is accepted.
Requirement IDs: designed `REQ-SCHEMA-011`, designed `REQ-BIND-003`, `REQ-MODEL-*`, designed `REQ-VAL-007`, `REQ-GEN-*`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/validation-architecture.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, runtime modules if accepted by planning, conformance tests, examples
Allowed files: public profile token plumbing required for `XP-XSD10-SEMANTIC`; parser/IR/binding/emitter/validation source and tests for accepted `nillable`, `default`, and `fixed` behavior; golden fixtures; interop fixtures; directly related docs
Forbidden files: substitution groups, mixed content, wildcards, XSD 1.1 assertions, dependency metadata unless approved by ADR
Expected behavior: add public profile token `XP-XSD10-SEMANTIC` and implement accepted `nillable`, `default`, and `fixed` semantics across generated model shape, reader/writer behavior, validation diagnostics, deterministic source, and round-trip/conformance fixtures.
Tests to add/update: `T-SEMANTIC-NIL-*`, `T-SEMANTIC-DEFAULT-*`, `T-SEMANTIC-FIXED-*`, generated source golden tests, generated compile tests, valid/invalid XML tests, default/fixed/nil diagnostics, round-trip tests, conformance fixtures, interop comparisons, and representative generated-code Native Image smoke coverage
Documentation to update: generated-code contract, validation architecture, conformance matrix, compatibility profiles, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: accepted semantic fixtures work end to end; model shape is stable and documented; interop evidence is recorded
Rollback notes: revert semantic implementation, tests, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare default/fixed/nillable behavior against approved XML Schema validation where practical.
- Native Image: generated semantic paths remain reflection-free.
- Security: diagnostics must not leak uncontrolled paths or secrets.
- Documentation: clearly separate absent, nil, defaulted, and fixed values.

## Accepted Implementation Scope

- Add public `XP-XSD10-SEMANTIC` profile plumbing across generator API, CLI help/parsing,
  Gradle plugin validation, CoreGenerator, and frontend profile gates.
- Accept `nillable="true"` only on required singleton elements whose resolved value type is already
  supported and is not list-valued. Bind as `Optional<T>`, with `Optional.empty()` representing
  explicit `xsi:nil`.
- Accept `default` and `fixed` only for supported scalar built-ins or accepted restricted scalar
  aliases on elements and attributes.
- Element defaults apply only to present empty simple elements; absent optional elements remain
  absent.
- Attribute defaults and fixed values are read as effective model values when absent.
- Reject optional or repeated nillable fields, nillable attributes, complex/list/union defaults,
  ambiguous nil/default/fixed combinations, unsupported scalar defaults, and unsupported lexical
  default/fixed values with deterministic diagnostics.
- Preserve existing generated-code constraints: no binding annotations, reflection, ServiceLoader,
  dynamic proxies, parser APIs in generated code, third-party runtime dependencies, or external
  resource access.
