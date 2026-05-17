# TASK-0026: composed-xsd10-planning

Status: accepted.

Task ID: `TASK-0026`
Gate: `0.3.0` Composed XSD 1.0 Schemas planning; starts only after `TASK-0025` is accepted.
Requirement IDs: future `REQ-SCHEMA-*`, future `REQ-BIND-*`, future `REQ-VAL-*`, `REQ-GEN-*`, `REQ-QA-001`
ADR IDs: `ADR-0006`, `ADR-0007`, `ADR-0008`, `ADR-0009`, `ADR-0013`
Specification references: `docs/compatibility-profiles.md`, `docs/conformance/matrix.md`, `docs/architecture/compiler-pipeline.md`, `docs/verification/conformance-strategy.md`
Target areas: requirements, architecture, conformance, verification, and task cards
Allowed files: requirements docs, architecture docs, conformance docs, verification docs, ADRs if required, agent task cards
Forbidden files: product implementation source, dependency metadata, generated output, build gate changes
Expected behavior: define the `0.3.0` composed-schema scope for named model groups, attribute groups, simple type list/union, and initial derivation; add requirement IDs, conformance rows, interop fixture candidates, unsupported diagnostics, and acceptance criteria.
Tests to add/update: planned tests only for `TASK-0027` through `TASK-0030`
Documentation to update: compatibility profiles, conformance matrix, architecture docs, traceability matrix, and follow-on task cards
Commands to run: `./gradlew validateDesignControlPack qualityGate`, `git diff --check`
Acceptance criteria: `0.3.0` support is decision-complete and still narrower than full XSD 1.0 unless explicitly accepted; interop candidates are identified for each feature group
Rollback notes: revert planning docs and task-card updates

## Impact Notes

- Interop: choose modular-schema fixtures that can be validated by external XML Schema tooling where practical.
- Native Image: define which composed-schema fixtures enter native lanes.
- Security: include composition depth and cycle considerations.
- Documentation: no full-XSD conformance claims.

## Accepted `0.3.0` Planning Scope

`TASK-0026` accepts planned opt-in profile `XP-XSD10-COMPOSED` for follow-on implementation tasks.
This task does not add the public API token or generator behavior.

`TASK-0027` shall implement only:

- Global `xs:group` declarations containing exactly one `xs:sequence` of already-supported
  particles.
- Direct `xs:group ref` use with `minOccurs=1` and `maxOccurs=1`, flattened into the containing
  content order.
- Global `xs:attributeGroup` declarations containing supported attributes.
- Direct `xs:attributeGroup ref` use, flattened into the containing type's attributes.

`TASK-0028` shall implement only:

- Named `xs:list` simple types with `itemType` resolving to a supported scalar built-in or accepted
  named restricted scalar alias.
- List-valued singleton elements and attributes bound as `List<T>` with explicit generated lexical
  splitting and validation.
- Named `xs:union` simple types with `memberTypes` resolving to supported scalar built-ins or
  accepted named restricted scalar aliases.
- Union-valued elements and attributes bound as lexical `String` with generated validator checks for
  accepted member alternatives.

`TASK-0029` shall implement only:

- Named complex-type `xs:complexContent/xs:extension` that flattens base fields before derived fields
  without generated Java inheritance.
- Named simple restriction derivation chains over supported scalar bases with merged accepted facet
  metadata.

Repeated or optional model-group references, nested model groups beyond the accepted direct shape,
`xs:all`, wildcards, anonymous list/union member types, `simpleContent`, complex restriction, mixed
content, abstract types, substitution groups, identity constraints, defaults/fixed semantics, and
full XSD 1.0 conformance remain out of scope.

Planned test identifiers are `T-GROUP-*`, `T-ATTRGROUP-*`, `T-LIST-*`, `T-UNION-*`,
`T-DERIVATION-*`, `T-CONF-XP-XSD10-COMPOSED-*`, and `T-INTEROP-COMPOSED-*`. Future implementation
tasks must include frontend, IR, binding, generated source, compile, reader/writer/validator,
unsupported diagnostics, deterministic emission, conformance/interop comparison against JDK XML
Schema validation, and representative Native Image smoke coverage where selected.

## Acceptance Evidence

- Compatibility, compiler pipeline, generated-code contract, validation architecture, conformance,
  verification, traceability, and follow-on task cards define the `0.3.0` scope without claiming
  full XSD 1.0 support.
- `TASK-0027` through `TASK-0030` are decision-complete enough for implementation agents to proceed
  without selecting profile shape, model shape, interop expectations, or excluded constructs.
- Repository verification passed with `./gradlew validateDesignControlPack qualityGate
  --console=plain` and `git diff --check`.
