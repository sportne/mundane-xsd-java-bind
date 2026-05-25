# Native Image sustainability review

`TASK-0076` reviewed the GraalVM Native Image posture after `XP-XSD10-FULL` became executable. The
review keeps Native Image as explicit opt-in evidence; the JVM `qualityGate` remains the default
project gate because native builds require a GraalVM toolchain and are materially slower.

## Local SDKMAN setup

The local supported setup in this workspace uses SDKMAN GraalVM:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
./gradlew validateDesignControlPack nativeSmoke nativeConformance --console=plain
```

The Gradle native lanes discover `native-image` from `JAVA_HOME/bin` or `PATH`. Sourcing SDKMAN
before Gradle starts is enough when the active SDKMAN Java candidate is GraalVM with Native Image
installed. If discovery fails, the native tasks stop before native compilation with the existing
toolchain diagnostic.

## Sustainability posture

| Area | Current posture | Evidence |
|---|---|---|
| Generated models/codecs | Generated records, sealed branch interfaces, readers, writers, and validators are static Java source. They do not use reflection, dynamic proxies, service loading, or runtime binding discovery. | generated-source architecture checks; `generatedCodeSmoke`; `generatedCodeNativeSmoke` |
| Runtime core | Runtime values and validation primitives avoid classpath scanning and reflection. Native tests execute runtime-core behavior directly. | `:modules:runtime-core:nativeTest` |
| JDK XML adapter | The optional JDK XML adapter uses JDK StAX and explicit adapter classes; secure resource denial remains covered under native execution. | `:modules:runtime-jdkxml:nativeTest`; `nativeConformance` |
| Example bindings | Purchase-order and multi-namespace generated bindings execute read/write/validate paths under Native Image. | `:examples:purchase-order:nativeTest`; `:examples:multi-namespace:nativeTest` |
| Selected conformance | `nativeConformance` builds selected generated bindings before image generation and runs static supported-profile round trips, unsupported diagnostics, and secure entity/resource denial in one executable. | `:modules:conformance-tests:nativeConformance` |
| Resource inclusion | Generated-code smoke does not need broad resource inclusion. Selected conformance includes `xml`, `xsd`, and `tsv` resources plus Xerces XML messages for JDK XML diagnostics. | `modules/generator-core/build.gradle`; `modules/conformance-tests/build.gradle` |
| CI behavior | GitHub Actions uses `graalvm/setup-graalvm` for Java 21 and Java 25 and runs `validateDesignControlPack nativeSmoke nativeConformance`. | `.github/workflows/native-image.yml` |

## Known warnings

The local SDKMAN GraalVM run completed with Native Image warnings for experimental
`-H:IncludeResources` and `-H:IncludeResourceBundles` options in resource-bearing lanes. Those
flags are intentionally limited to native tests/conformance because selected fixture XML/XSD/TSV
resources and JDK XML parser message bundles must be present in the native executable. They are not
product runtime dependencies and do not add reflection/proxy configuration.

Future Native Image upkeep should monitor GraalVM's guidance for replacing or explicitly unlocking
those experimental resource flags, and should consider `--strict-image-heap` only as a future
toolchain-hardening task after measuring impact.
