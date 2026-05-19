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

## `0.5.0` unknown-content security evidence

`TASK-0036` plans security coverage for accepted `XP-XSD10-DOCUMENT` open content. `TASK-0037`
proves retained unknown XML does not weaken resolver or parser policy, does not retain parser
handles or external resources, and preserves deterministic diagnostics for unsupported unknown
content shapes. `TASK-0038` adds mixed-content text and retained-fragment nesting coverage,
`TASK-0039` verifies serialization output does not leak local diagnostic or resolver paths, and
`TASK-0040` records readiness evidence without expanding unknown-content behavior.
