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

## First implementation requirement

The first schema resolver PR must include negative tests proving denied network access and include/import cycle detection.
