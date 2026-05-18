# TASK-0031: xsd10-semantic-expansion-planning

Status: accepted.

Task ID: `TASK-0031`
Gate: `0.4.0` XSD 1.0 Semantic Expansion planning; starts only after `TASK-0030` is accepted.
Requirement IDs: designed `REQ-SCHEMA-011`, designed `REQ-SCHEMA-012`, designed `REQ-BIND-003`, designed `REQ-VAL-007`, `REQ-GEN-*`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0013`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/architecture/validation-architecture.md`
Target areas: requirements, architecture, conformance, verification, and task cards
Allowed files: requirements docs, architecture docs, conformance docs, verification docs, ADRs if required, agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define the `0.4.0` semantic scope for `nillable`, `default`, `fixed`, substitution groups, and broader validation semantics; record requirements, model-shape decisions, interop fixture candidates, unsupported diagnostics, and acceptance criteria.
Tests to add/update: planned tests only for `TASK-0032` through `TASK-0035`
Documentation to update: compatibility profiles, generated-code contract, validation architecture, conformance matrix, traceability matrix, follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: semantic behavior is decision-complete before implementation; any generated model shape changes are documented; interop candidates are identified
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose semantic fixtures where external validators can verify value/default/nillable outcomes.
- Native Image: define representative semantic fixtures for native lanes.
- Security: validation diagnostics must remain path-safe.
- Documentation: do not mix document-oriented open content into this slice.

## Accepted `0.4.0` Planning Scope

`TASK-0031` accepts planned opt-in profile `XP-XSD10-SEMANTIC` for follow-on implementation tasks.
This task does not add the public API token or generator behavior. `TASK-0032` is the first approved
task to add the public generator profile token across API, CLI, Gradle plugin, and CoreGenerator.

`TASK-0032` shall implement only:

- `nillable="true"` on required singleton elements whose resolved value type is already supported
  and is not list-valued. Generated binding uses `Optional<T>` where `Optional.empty()` means the
  XML value was explicitly `xsi:nil`.
- `default` and `fixed` for supported scalar built-ins or accepted restricted scalar aliases on
  elements and attributes.
- Element defaults apply only to present empty simple elements; absent optional elements remain
  absent. Attribute defaults and fixed values are read as effective model values when attributes are
  absent.
- Optional or repeated nillable fields, nillable attributes, complex/list/union defaults, and
  ambiguous nil/default/fixed combinations must fail with deterministic diagnostics.

`TASK-0033` shall implement only:

- Direct global `xs:element substitutionGroup="head"` members.
- Singleton references to accepted substitution group heads.
- Generated sealed branch models with one record branch per accepted head or member element,
  preserving the actual XML element name for reader/writer dispatch.
- Nested substitution groups, cycles, blocking/final semantics, wildcards, mixed content, repeated
  substitution groups, full polymorphism, and abstract complex types must fail with deterministic
  diagnostics.

`TASK-0034` shall implement only generated validation hardening for accepted `0.4.0` behavior:
nil content rules, fixed-value checks, default/fixed reader behavior, substitution dispatch
diagnostics, deterministic diagnostic ordering, and interop comparison.

Identity constraints, wildcards, mixed content, full datatype semantics, full derivation semantics,
XSD 1.1, dependency changes, release tags, artifact publication, and full XSD 1.0 conformance remain
out of scope.

Planned test identifiers are `T-SEMANTIC-NIL-*`, `T-SEMANTIC-DEFAULT-*`,
`T-SEMANTIC-FIXED-*`, `T-SUBSTITUTION-*`, `T-SEMANTIC-VALIDATION-*`,
`T-CONF-XP-XSD10-SEMANTIC-*`, and `T-INTEROP-SEMANTIC-*`. Future implementation tasks must include
frontend/profile gating, IR, binding, generated source, compile, reader/writer/validator behavior,
unsupported diagnostics, deterministic emission, conformance/interop comparison against JDK XML
Schema validation where practical, and representative Native Image smoke coverage.

## Acceptance Evidence

- Compatibility profiles, compiler pipeline, generated-code contract, validation architecture,
  conformance matrix, verification plan, traceability matrix, and follow-on task cards define the
  `0.4.0` planning scope without claiming full XSD 1.0 support.
- `TASK-0032` through `TASK-0035` are decision-complete enough for implementation agents to proceed
  without selecting profile shape, model shape, interop expectations, or excluded constructs.
- Repository verification passed with `./gradlew validateDesignControlPack qualityGate
  --console=plain` and `git diff --check`.
