# TASK-0039: canonicalization-serialization-policy

Status: draft.

Task ID: `TASK-0039`
Gate: `0.5.0` Document-Oriented and Open Content; starts only after `TASK-0038` is accepted.
Requirement IDs: future accepted XML writer, XML reader, generation, conformance, and interop IDs
ADR IDs: `ADR-0005`, `ADR-0008`, `ADR-0009`, `ADR-0011`, `ADR-0013`
Specification references: `docs/architecture/generated-code-contract.md`, `docs/architecture/runtime-architecture.md`, `docs/verification/conformance-strategy.md`
Target modules: generator-core, runtime modules if accepted, conformance tests, examples
Allowed files: serialization policy tests/source for accepted behavior, golden XML/source fixtures, interop fixtures, directly related docs
Forbidden files: cryptographic XML canonicalization claims unless separately approved, broad serializer rewrites outside accepted policy, dependency metadata unless approved
Expected behavior: add accepted canonicalization and serialization-policy tests for generated XML output, including namespace prefix policy, attribute ordering if controlled, text handling, open/mixed content serialization, and stable interop expectations.
Tests to add/update: XML output golden tests, round-trip normalization tests, interop serialization comparisons where practical, negative tests for unsupported canonicalization claims
Documentation to update: generated-code contract, runtime architecture, conformance matrix, verification plan, traceability matrix
Commands to run: `./gradlew validateDesignControlPack qualityGate`, targeted generator/conformance/example checks, `git diff --check`
Acceptance criteria: serialization behavior is stable and documented; no unsupported canonical XML claim is made; interop evidence is recorded
Rollback notes: revert serialization policy tests/source, fixtures, golden outputs, and docs

## Impact Notes

- Interop: compare generated XML with approved tool output where useful without requiring byte-identical lexical forms unless accepted.
- Native Image: serialization policy must work in native lanes selected for the slice.
- Security: output behavior must not leak local diagnostic or resolver paths.
- Documentation: distinguish stable project output from formal XML canonicalization.
