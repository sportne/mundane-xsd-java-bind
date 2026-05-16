# generator-core

Schema compiler implementation module.

## Current status

Initial schema resource resolution and raw XSD syntax frontend behavior are implemented. Component graph, normalized IR, binding, and source emission remain gated by approved task cards.

## Contributor notes

- Keep schema resolution, parsed schema model, binding model, and source emission as documented architecture concepts.
- Add golden-output and round-trip verification when generation behavior starts.
- Do not leak generator implementation dependencies into runtime modules or generated code.
