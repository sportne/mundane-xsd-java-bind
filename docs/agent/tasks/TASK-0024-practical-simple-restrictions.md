# TASK-0024: practical-simple-restrictions

Status: draft.

Task ID: `TASK-0024`
Gate: `0.2.0` Practical Data Contracts; starts only after `TASK-0023` is accepted.
Requirement IDs: `REQ-VAL-003`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-XML-R-001`, `REQ-XML-W-001`, `REQ-MODEL-001`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/validation-architecture.md`, `docs/architecture/generated-code-contract.md`, `docs/conformance/matrix.md`
Target modules: `modules/generator-core`, conformance tests, examples as approved by `TASK-0022`
Allowed files: simple-type parsing/IR/binding/validation/emitter source and tests for accepted enumeration, string length, numeric inclusive range, and string pattern behavior; golden fixtures; conformance fixtures; directly related docs
Forbidden files: full datatype system, list/union unless approved in a later slice, identity constraints, dependency metadata, runtime dependency additions, XSD 1.1 assertions
Expected behavior: expand practical simple restrictions for generated validation and diagnostics, covering the accepted `TASK-0022` enumeration, string length, numeric inclusive range, and string pattern subset with deterministic generated source and runtime behavior.
Tests to add/update: golden frontend/IR/binding/source tests, generated compile tests, valid/invalid lexical and facet tests, diagnostics tests with locations where available, round-trip tests preserving accepted lexical semantics, representative Native Image smoke fixtures, and interop comparisons where practical
Documentation to update: validation architecture, conformance matrix, compatibility profiles, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks named by the implementation, `git diff --check`
Acceptance criteria: accepted facets are generated and tested; unsupported facets produce explicit diagnostics; JDK/XML interop evidence is recorded where practical
Rollback notes: revert simple restriction implementation, tests, fixtures, golden outputs, and directly related docs

## Impact Notes

- Interop: compare representative valid/invalid facet fixtures with approved XML Schema validation where practical.
- Native Image: generated validators must remain explicit and reflection-free.
- Security: pattern handling must avoid unbounded or unsafe behavior.
- Documentation: keep support claims facet-specific.

## Accepted `TASK-0022` Scope

Implement only named `xs:simpleType` restrictions whose base is one of the existing supported scalar
mappings: `xs:string`, `xs:boolean`, `xs:int`, `xs:integer`, `xs:long`, or `xs:decimal`.

Accepted facets are `xs:enumeration` for all accepted scalar bases; `xs:length`, `xs:minLength`,
and `xs:maxLength` for `xs:string`; `xs:minInclusive` and `xs:maxInclusive` for numeric scalar
bases; and `xs:pattern` for `xs:string` only. Multiple facets in accepted categories combine
conjunctively.

Unsupported facets, `xs:list`, `xs:union`, derivation chains, anonymous simple types, whitespace
normalization beyond current lexical conversion, timezone-aware date/time types, locale-sensitive
behavior, and XSD 1.1 assertions must continue to produce deterministic unsupported-profile
diagnostics.

Facet validation must appear in generated object validation and XML validation. Generated readers
remain responsible for scalar lexical conversion before validators apply facet checks. Generated
pattern checks must use JDK regular expressions, report deterministic invalid-pattern schema
diagnostics, and avoid third-party dependencies, reflection, dynamic proxies, and ServiceLoader.

Planned test identifiers are `T-FACET-FRONTEND-*`, `T-FACET-IR-*`, `T-FACET-BIND-*`,
`T-FACET-SOURCE-*`, `T-FACET-VALIDATOR-*`, `T-FACET-DIAGNOSTIC-*`, `T-RT-FACET-*`,
`T-CONF-XP-VALIDATION-10-BASIC-*`, and `T-INTEROP-FACET-*`.
