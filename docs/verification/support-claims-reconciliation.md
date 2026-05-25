# Support claims reconciliation

## TASK-0067 finding

`TASK-0067` reviewed public `1.0.0`, `XP-XSD10-FULL`, and "full XSD 1.0" wording against
`docs/conformance/matrix.md`, `docs/verification/xsd10-full-feature-matrix.md`, and the
`TASK-0066` release evidence.

The accepted claim is:

- `XP-XSD10-FULL` is executable for the project's accepted XML Schema 1.0 generated-binding product
  scope.
- Generated binding execution is supported through the API, CLI, Gradle plugin, CoreGenerator,
  selected local fixtures, generated-code smoke, and Native Image lanes where documented.
- W3C XML Schema 1.0 suite evidence is opt-in classification plus explicitly mapped
  generated-binding rows, not a broad W3C full-suite generated-binding pass.

The docs must not imply:

- standalone generic XML Schema validation independent of generated bindings
- W3C full-suite generated-binding coverage beyond explicitly mapped rows
- XSD 1.1 or XML 1.1 support
- XML Canonicalization, XML Signature canonical forms, lexical prefix preservation, DTD/entity
  identity, DOM-backed binding, Maven Central publication, signing, or hard performance guarantees
