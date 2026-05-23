# TASK-0053: xsd10-derivation-polymorphism

Status: accepted.

Task ID: `TASK-0053`
Gate: accepted XSD 1.0 derivation, substitution, and dynamic typing expansion toward `XP-XSD10-FULL`.
Target areas: component graph, IR normalization, binding model, generated model/reader/writer/validator emitters, tests, docs
Allowed files: generator-core schema/bind/emit/tests, runtime-core only if dynamic dispatch needs public helpers, conformance fixtures, docs
Forbidden files: identity constraints, XML Canonicalization, release metadata, XSD 1.1/XML 1.1
Expected behavior: implement the accepted derivation/polymorphism slice: simpleContent text-with-attributes binding for accepted simple bases, normalized simpleContent restrictions, basic complexContent restriction checks against base members, repeated and nested substitution-group heads, abstract substitution heads with concrete branches only, deterministic substitution cycle diagnostics, and generated read/write/validate behavior for those accepted shapes. Preserve `XP-XSD10-FULL` as non-executable.
Tests to add/update: IR normalization tests, binding shape tests, generated reader/writer/validator tests, selected JDK XML Schema comparison fixtures, unsupported invalid derivation diagnostics.
Acceptance criteria: generated binding supports the accepted TASK-0053 derivation and substitution surfaces while preserving deterministic diagnostics for shapes still outside the executable profile.
Rollback notes: revert polymorphic model and derivation-normalization changes from this task.

## Completion notes

Accepted on 2026-05-23.

Implementation evidence:

- Added IR metadata for abstract global elements and simpleContent value content.
- Expanded substitution-group normalization to support abstract heads, repeated head references, nested members, and cycle diagnostics.
- Added binding and generated-code support for simpleContent model records with a `value` field plus declared attributes.
- Added generated reader/writer/validator support for simpleContent text values.
- Added basic complexContent restriction normalization that accepts restricted particles/attributes already present in the base and rejects unknown restricted names.
- Added selected conformance fixtures:
  - `T-CONF-XP-XSD10-COMPOSED-SIMPLE-CONTENT`
  - `T-CONF-XP-XSD10-SEMANTIC-SUBSTITUTION-REPEATED`

Verification run:

- `./gradlew :modules:generator-core:check --console=plain`
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew :modules:generator-core:check :modules:generator-core:generatedCodeSmoke --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`

Known remaining work for later full-XSD gates:

- `xsi:type` dispatch for known derived types.
- Full block/final and schema default enforcement.
- Complete XSD complex restriction algebra.
- Full simpleContent restriction against complex simple-content bases.
- Generated polymorphic declared-base fields beyond substitution group branch dispatch.
- Identity constraints, full W3C suite intake, and final `XP-XSD10-FULL` execution.
