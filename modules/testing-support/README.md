# testing-support

Test helpers for generated bindings.

## Current status

Scaffold only. This module is intended for downstream users testing generated model, reader, writer, and validation behavior.

## Contributor notes

- Keep helpers small and focused on generated-code verification.
- Runtime helpers may depend on `runtime-core`; avoid generator dependencies.
- Document every helper package with contributor-facing examples.
