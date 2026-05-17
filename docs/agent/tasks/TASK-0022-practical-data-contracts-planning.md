# TASK-0022: practical-data-contracts-planning

Status: accepted.

Task ID: `TASK-0022`
Gate: `0.2.0` Practical Data Contracts planning; starts only after `TASK-0021` is accepted.
Requirement IDs: `REQ-SCHEMA-007`, `REQ-VAL-003`, `REQ-GEN-001`, `REQ-GEN-002`, `REQ-XML-W-001`, `REQ-XML-R-001`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0011`, `ADR-0013`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/verification/conformance-strategy.md`, `docs/verification/verification-plan.md`
Target areas: requirements, conformance matrix, verification docs, and draft task cards
Allowed files: requirements docs, conformance docs, verification docs, architecture docs, ADRs if scope changes require them, and agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define the exact `XP-DATA-10-CHOICE` and `XP-VALIDATION-10-BASIC` support for `0.2.0`, including accepted choice shapes, accepted simple restriction facets, test IDs, interop fixture candidates, unsupported diagnostics, and readiness criteria.
Tests to add/update: planned tests only; record expected golden, round-trip, negative, Native Image, and interop evidence for `TASK-0023` through `TASK-0025`
Documentation to update: compatibility profiles, conformance matrix, verification plan, traceability matrix, and follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: `0.2.0` has decision-complete task cards; no implementation is authorized by this task; interop expectations are listed for each accepted feature where a JDK/XML reference can be useful
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose concrete `0.2.0` fixtures for comparison against JDK XML validation or other approved references where practical.
- Native Image: define whether new choice/facet fixtures join smoke or conformance lanes.
- Security: preserve existing resolver and reader denial policies.
- Documentation: avoid claiming `XP-XSD10-FULL`.

## Accepted `0.2.0` Planning Scope

`TASK-0022` accepts the `0.2.0` Practical Data Contracts slice as a documentation and planning gate only.
It does not authorize release tagging, artifact publication, build metadata changes, or product behavior changes.
The current repository remains `0.1.0-SNAPSHOT` readiness evidence until a later release-engineering task
approves a publication-ready release process.

`TASK-0023` shall implement only the following `XP-DATA-10-CHOICE` shapes:

- Local `xs:choice` particles in a complex type content model, either as the only content particle or as an item in an enclosing supported `xs:sequence`.
- Choice particles with `minOccurs` of `0` or `1` and `maxOccurs` of `1`.
- Branches that are local `xs:element` declarations or references to global elements whose resolved type is already supported by `XP-DATA-10`.
- Branch element cardinality remains the default singleton cardinality; repeated branch elements, nested model groups, wildcards, substitution groups, mixed content, anonymous branch complex types, and repeated choices remain out of scope.
- Generated model binding uses one field per choice particle. The field type is a generated sealed interface named `<ContainingTypeSimpleName>Choice`; each branch is a generated record named `<BranchElementSimpleName>Choice` carrying the existing scalar or model value for that branch. Optional choices use `Optional<<ContainingTypeSimpleName>Choice>`.
- Generated readers must accept exactly one branch for required choices, zero or one branch for optional choices, preserve sequence order around surrounding particles, and report deterministic diagnostics for missing, repeated, out-of-order, or unknown branch elements.
- Generated writers emit the selected branch in binding order and must not introduce reflection, annotations, ServiceLoader, dynamic proxies, DOM-first processing, or new runtime dependencies.

`TASK-0024` shall implement only the following `XP-VALIDATION-10-BASIC` simple restriction facets:

- Named `xs:simpleType` restrictions whose base is one of the existing supported scalar mappings: `xs:string`, `xs:boolean`, `xs:int`, `xs:integer`, `xs:long`, or `xs:decimal`.
- `xs:enumeration` for all accepted scalar bases.
- `xs:length`, `xs:minLength`, and `xs:maxLength` for `xs:string`.
- `xs:minInclusive` and `xs:maxInclusive` for numeric scalar bases already supported by the binding model.
- `xs:pattern` for `xs:string` only, compiled through JDK regular expressions in generated validation code with deterministic invalid-pattern schema diagnostics.
- Multiple facets of the same accepted category combine conjunctively. Unsupported facets, list/union, derivation chains, anonymous simple types, whitespace normalization beyond current lexical conversion, timezone-aware date/time types, and locale-sensitive behavior remain out of scope.
- Facet validation must appear in generated object validation and XML validation. Reader lexical conversion remains responsible for scalar parsing before facet checks.
- Pattern handling must avoid introducing third-party dependencies or runtime reflection and must keep diagnostics stable.

## Planned Evidence

- `TASK-0023` must add frontend, IR, binding, generated source golden, compile, reader/writer, validator, negative diagnostic, round-trip, conformance, and interop tests with `T-CHOICE-*` identifiers.
- `TASK-0024` must add frontend, IR, binding, generated source golden, compile, validation, negative diagnostic, round-trip, conformance, and interop tests with `T-FACET-*` identifiers.
- At least one positive and one negative fixture for each accepted feature group must be compared against JDK XML Schema validation where practical.
- New choice and facet fixtures must join the normal JVM quality gate. Native Image coverage remains smoke-level for representative generated bindings in `TASK-0023` and `TASK-0024`; broader Native Image conformance remains reserved for `TASK-0044`.
- `TASK-0025` must verify that `0.2.0` support claims match implemented evidence and that no `0.1.0` release tag or publication claim was introduced by this planning gate.

## Completion Notes

- Promoted `TASK-0022` to accepted after defining the decision-complete `0.2.0` Practical Data
  Contracts planning scope.
- Recorded accepted `TASK-0023` choice shapes, generated model shape, unsupported diagnostics,
  interop expectations, and Native Image smoke expectations.
- Recorded accepted `TASK-0024` simple restriction facets, unsupported diagnostics, interop
  expectations, and Native Image smoke expectations.
- Updated compatibility, conformance, validation architecture, generated-code contract,
  verification, traceability, requirements, readiness, and handoff docs without changing product
  source, dependency metadata, build gates, generated output, release tags, or publication posture.
- Verification: `./gradlew validateDesignControlPack qualityGate` passed.
