# Native Image test plan

## Purpose

Native Image tests prove that generated bindings and runtime paths avoid unexpected dynamic JVM behavior.

## Stages

1. **Wiring stage:** native plugin configured; no product tests yet.
2. **Runtime primitive stage:** once `runtime-core` has executable production behavior, `:modules:runtime-core:nativeTest` must run for the task that adds it or the task must document a concrete blocker.
3. **Generated-source harness stage:** once generated code and runtime primitives can execute together, the generated-source verification harness must include a Native Image smoke path or document a concrete blocker.
4. **Round-trip stage:** generated XML read/write tests run in native executables for representative examples.
5. **Conformance stage:** selected profile tests run in native executables.

## Trigger policy

Native Image checks are not part of the default `qualityGate` while there is no meaningful executable runtime or generated binding. They become mandatory task evidence at the first task that creates each executable surface:

- `TASK-0010` must run `:modules:runtime-core:nativeTest` once runtime-core primitives have behavior to execute.
- `TASK-0013` adds `:modules:generator-core:generatedCodeNativeSmoke`, the first generated-code native smoke path for approved generated model/writer fixtures plus `runtime-core`.
- `TASK-0017` must reuse representative round-trip fixtures for native smoke execution.
- `TASK-0020` hardens and broadens native checks; it must not be the first point where generated/runtime Native Image compatibility is exercised.

## Active generated-code command

Run the generated-code native smoke task with GraalVM native-image available:

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.2-graalce \
PATH=$HOME/.sdkman/candidates/java/21.0.2-graalce/bin:$PATH \
./gradlew :modules:generator-core:generatedCodeNativeSmoke --console=plain
```

If `native-image` is not on `PATH` and `JAVA_HOME` does not point to a GraalVM installation with `native-image`, the task fails with a concrete toolchain message before attempting a native build.

## Failure policy

A Native Image failure caused by reflection, resource lookup, proxy generation, serialization metadata, or classpath scanning must be treated as an architecture issue unless explicitly approved by ADR.

JVM-only verification tools, such as architecture-analysis engines, should remain in the normal JVM `check` lane and must not be required inside native smoke executables unless the tool itself is part of the supported runtime surface.
