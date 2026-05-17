# generator-core

Schema compiler implementation module.

## Current status

Initial schema resource resolution, raw XSD syntax frontend behavior, component graph, normalized
IR, binding, generated model/writer/reader/validator emission, public generator-core adapter
behavior, generated-source verification harness behavior, and generated-code Native Image smoke
coverage are implemented for the accepted `XP-DATA-10` slice and the accepted opt-in
`XP-DATA-10-CHOICE` and `XP-VALIDATION-10-BASIC` readiness subsets.

## Contributor notes

- Keep schema resolution, parsed schema model, binding model, and source emission as documented architecture concepts.
- Use the generated-source verification harness for golden-output, deterministic emission, compile, JVM smoke, and generated-code native smoke coverage.
- Do not leak generator implementation dependencies into runtime modules or generated code.
