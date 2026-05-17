# generator-core

Schema compiler implementation module.

## Current status

Initial schema resource resolution, raw XSD syntax frontend behavior, component graph, normalized IR, binding, generated model/writer emission, and generated-source verification harness behavior are implemented.

## Contributor notes

- Keep schema resolution, parsed schema model, binding model, and source emission as documented architecture concepts.
- Use the generated-source verification harness for golden-output, deterministic emission, compile, JVM smoke, and generated-code native smoke coverage.
- Add round-trip verification when generated reader behavior starts.
- Do not leak generator implementation dependencies into runtime modules or generated code.
