# Module boundaries

| Module | Responsibility | Runtime dependency policy |
|---|---|---|
| `runtime-core` | Names, diagnostics, locations, XML event/output interfaces, validation error model. | No third-party dependencies. |
| `runtime-jdkxml` | Optional adapters between JDK XML APIs and runtime-core interfaces. | JDK `java.xml`; no third-party dependencies. |
| `generator-api` | Public generator configuration, profile, and extension API. | Generator dependencies allowed if not runtime-visible. |
| `generator-core` | Schema resolver, parser frontend, component graph, IR, binding engine, emitters. | Generator dependencies allowed. |
| `generator-cli` | CLI entry point. | Depends on generator API/core. |
| `generator-gradle-plugin` | Gradle generation integration. | Gradle APIs plus generator API/core; no runtime dependency. |
| `testing-support` | User-facing helpers for generated binding tests. | Test dependencies allowed. |
| `conformance-tests` | Internal conformance and differential test harness. | Test dependencies allowed. |
| `examples:*` | Example schemas and generated-code workflows. | Not published. |

## Forbidden dependencies

- `runtime-core` must not depend on generator modules.
- Generated code must not depend on generator modules.
- Generated code must not depend on third-party libraries.
- `generator-api` must not expose implementation parser types.
- `runtime-jdkxml` must not become required by generated code.
