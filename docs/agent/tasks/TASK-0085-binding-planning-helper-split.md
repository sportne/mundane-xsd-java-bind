# TASK-0085: binding-planning-helper-split

Status: accepted.

Task ID: `TASK-0085`
Priority: P2
Gate: generator architecture refactor.
Requirement IDs: `REQ-BIND-001`, `REQ-GEN-001`, `REQ-QA-002`
ADR IDs: `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/complexity-review.md`,
`docs/architecture/generated-code-contract.md`, `docs/verification/naming-collision-review.md`
Target module: `modules/generator-core`
Allowed files: generator-core binding builder/helper classes and tests, architecture/naming docs,
traceability docs, and this task/handoff.
Forbidden files: public binding model API changes, new customization syntax, generated output
changes, schema support expansion, dependency metadata, release metadata, and quality-gate
weakening.
Expected behavior: split deterministic binding naming and grouped-content planning into focused
package-private helpers while preserving `BindingModelBuilder` output shape, ordering, diagnostics,
and generated source behavior.
Tests to add/update: focused tests for duplicate local type names, inline type names, field
collisions, grouped content planning, wildcard fields, and namespace-to-package mapping.
Documentation to update: complexity review, naming collision review if helper ownership is
clarified, traceability, and handoff.
Commands to run: `./gradlew :modules:generator-core:check --console=plain`,
`./gradlew validateDesignControlPack qualityGate --console=plain`, and `git diff --check`.
Acceptance criteria: package-private naming/content helpers exist; existing binding and generated
source behavior remains unchanged; no new customization API or support claim is added.
Rollback notes: revert helper extraction, focused tests, and docs.

Completion notes:
- Extracted package-private `BindingNameAllocator` from `BindingModelBuilder` to own Java package
  derivation, type-name suffix allocation, field-name collision helpers, and binding configuration
  diagnostics.
- Extracted package-private `BindingContentPlanner` to own mixed/grouped content-list field
  planning, branch ordering, grouped-position metadata, wildcard branch metadata, and composed
  branch cardinality helpers.
- Kept `BindingModelBuilder` responsible for schema indexing, global declaration lookup,
  type-reference binding, substitution/dynamic branch semantics, validation-plan text, and public
  `BindingModel` shape.
- Added focused allocator/planner tests for duplicate local names, inline/generated type-name
  collisions, package mapping, field collisions, grouped content positions, wildcard branch
  metadata, and mixed-content branch planning.

Evidence:
- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
