# generator-core

Schema compiler implementation module.

## Current status

Scaffold only. Product compiler behavior remains gated by the design-control process.

## Contributor notes

- Keep schema resolution, parsed schema model, binding model, and source emission as documented architecture concepts.
- Add golden-output and round-trip verification when generation behavior starts.
- Do not leak generator implementation dependencies into runtime modules or generated code.
