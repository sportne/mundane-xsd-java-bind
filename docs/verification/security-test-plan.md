# Security test plan

## Test categories

- External entity disabled by default.
- Network resolver denied by default.
- Include/import cycle detection.
- Maximum schema/include depth.
- Maximum XML nesting depth.
- Oversized token/text handling.
- Diagnostic path sanitization.
- Explicit opt-in for integration tests requiring network resources.

## Active evidence

The first public vertical slice includes resolver tests for denied network access, local-root and
catalog policy, and include/import cycle detection. It also includes runtime-jdkxml tests for
external entity denial and generated-validator example coverage for secure adapter behavior.

Depth, oversized-token, path-sanitization, and explicitly networked integration scenarios remain
future hardening work unless a task card accepts them.
