# TASK-0050: xsd10-datatype-facet-engine

Status: accepted.

Task ID: `TASK-0050`
Gate: full XSD 1.0 datatype and facet engine.
Target areas: runtime-core value types, generator-core scalar typing, reader/writer lexical conversion, generated validation, tests, docs
Allowed files: runtime-core, runtime-jdkxml tests where needed, generator-core schema/bind/emit/tests, conformance fixtures, docs
Forbidden files: content-model automata, full derivation/polymorphism, identity constraints, external dependencies without ADR, XSD 1.1/XML 1.1
Expected behavior: implement all XML Schema 1.0 built-in simple types, derived built-ins, and restriction facets with deterministic lexical conversion and generated validation.
Tests to add/update: datatype unit tests, facet combination tests, generated reader/writer/validator tests, JUnit integration-style conformance fixtures.
Acceptance criteria: XSD 1.0 primitive and derived built-ins plus all XSD 1.0 facets are covered by executable tests or explicitly documented non-binding metadata behavior.
Rollback notes: revert runtime datatype additions, scalar binding changes, and fixture updates from this task.

## Completion notes

Accepted in this task:

- Added runtime-core XML Schema value types for duration, date/time fragments, binary values,
  anyURI, and QName/NOTATION, plus the shared `XmlDatatypes` lexical/facet engine.
- Extended generated scalar mapping, readers, writers, and validators to use the shared datatype
  engine for all XML Schema 1.0 built-ins and derived built-ins in currently accepted schema shapes.
- Added namespace-aware QName parsing support through `XmlEventReader.namespaceUriForPrefix(...)`
  and QName output support through `XmlOutput.qNameText(...)`; the JDK XML adapter implements both.
- Added syntax/IR/binding support for `whiteSpace`, exclusive bounds, `totalDigits`, and
  `fractionDigits`, while preserving stable generated diagnostic categories for existing facets.
- Added selected conformance fixture `T-CONF-XP-XSD10-COMPOSED-DATATYPES` comparing JDK XML Schema
  validation with generated read/write/validate behavior for datatype families and facets.

Out of scope remains unchanged: content-model automata, full attributes/wildcards, full derivation
and dynamic typing, identity constraints, XSD 1.1/XML 1.1, release publication, release tags, and
making `XP-XSD10-FULL` executable.

Verification evidence:

- `./gradlew :modules:runtime-core:check :modules:runtime-jdkxml:check :modules:generator-core:check --console=plain`
- `./gradlew :modules:conformance-tests:check --console=plain`
- `./gradlew :modules:generator-core:generatedCodeSmoke --console=plain`
- `./gradlew validateDesignControlPack qualityGate --console=plain`
- `git diff --check`
