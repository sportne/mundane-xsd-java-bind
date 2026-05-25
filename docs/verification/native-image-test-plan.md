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

Native Image checks are not part of the default `qualityGate` because they require GraalVM native-image and are materially slower than the JVM lane. They become mandatory task evidence at the first task that creates each executable surface:

- `TASK-0010` must run `:modules:runtime-core:nativeTest` once runtime-core primitives have behavior to execute.
- `TASK-0013` adds `:modules:generator-core:generatedCodeNativeSmoke`, the first generated-code native smoke path for approved generated model/writer fixtures plus `runtime-core`.
- `TASK-0017` reuses representative purchase-order and multi-namespace round-trip fixtures through
  `:examples:purchase-order:nativeTest` and `:examples:multi-namespace:nativeTest`.
- `TASK-0020` adds the root `nativeSmoke` aggregate and makes the CI Native Image workflow run that aggregate; it must not be the first point where generated/runtime Native Image compatibility is exercised.
- `TASK-0044` adds the selected `nativeConformance` aggregate beside `nativeSmoke` for the
  `0.6.0` hardening lane.

## Active native smoke command

Run the representative Native Image smoke lane with GraalVM native-image available:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
./gradlew validateDesignControlPack nativeSmoke --console=plain
```

The `nativeSmoke` aggregate depends on runtime-core native tests, runtime-jdkxml native tests,
the generator-core generated-code native smoke executable, and the purchase-order and
multi-namespace example native tests. CI runs the same native aggregate together with
`validateDesignControlPack` on the GraalVM Java 21 and Java 25 matrix.

The generator-core generated-code smoke executable includes representative `XP-DATA-10-CHOICE`,
`XP-VALIDATION-10-BASIC`, `XP-XSD10-COMPOSED` group/attribute-group, list/union, and derivation
paths, plus `XP-XSD10-SEMANTIC` nillable/default/fixed, direct substitution-group, and generated
semantic validation paths, plus an `XP-XSD10-DOCUMENT` mixed/open-content serialization path, as
accepted readiness evidence through `TASK-0040`. Broader Native Image conformance remains a later
hardening task.

If `native-image` is not on `PATH` and `JAVA_HOME` does not point to a GraalVM installation with `native-image`, the task fails with a concrete toolchain message before attempting a native build.

## Active selected conformance command

Run the selected Native Image conformance lane with GraalVM native-image available:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
./gradlew validateDesignControlPack nativeConformance --console=plain
```

The `nativeConformance` aggregate depends on
`:modules:conformance-tests:nativeConformance`. That task builds one executable that reuses the
`TASK-0042` selected fixtures, compiles generated bindings at build time, and executes static
read/write/validate round trips for `XP-DATA-10`, `XP-DATA-10-CHOICE`,
`XP-VALIDATION-10-BASIC`, `XP-XSD10-COMPOSED`, `XP-XSD10-SEMANTIC`, and
`XP-XSD10-DOCUMENT` wildcard/mixed content. The same executable also checks selected
unsupported-diagnostic schemas and secure adapter entity/resource denial.

`nativeConformance` remains separate from `nativeSmoke` and the default JVM `qualityGate`. CI runs
`./gradlew validateDesignControlPack nativeSmoke nativeConformance --console=plain` only in the
GraalVM Native Image workflow.

If `native-image` is unavailable locally, `:modules:conformance-tests:checkNativeConformanceToolchain`
fails before attempting native compilation with the same concrete toolchain message as the
generated-code smoke lane.

For post-`1.0.0` local evidence, `TASK-0076` uses the combined SDKMAN GraalVM command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
./gradlew validateDesignControlPack nativeSmoke nativeConformance --console=plain
```

`docs/verification/native-image-sustainability-review.md` records why the generated and runtime
paths remain Native Image friendly, which resource flags are native-lane-only, and which GraalVM
warnings should be watched in future toolchain maintenance.

## Failure policy

A Native Image failure caused by reflection, resource lookup, proxy generation, serialization metadata, or classpath scanning must be treated as an architecture issue unless explicitly approved by ADR.

JVM-only verification tools, such as architecture-analysis engines, should remain in the normal JVM `check` lane and must not be required inside native smoke executables unless the tool itself is part of the supported runtime surface.
