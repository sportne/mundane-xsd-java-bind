# CI plan

## Workflows

| Workflow | Purpose |
|---|---|
| `ci.yml` | Java 21 and Java 25 build/check matrix. |
| `native-image.yml` | GraalVM Native Image lane. |
| `docs.yml` | Documentation and design-control checks. |
| `nightly.yml` | Scheduled clean quality-gate run. |

## Required CI stages

1. Checkout.
2. Setup JDK.
3. Setup Gradle cache.
4. Run `./gradlew qualityGate` in the main CI matrix.
5. Run `./gradlew validateDesignControlPack` in documentation lanes.
6. Run `./gradlew validateDesignControlPack nativeSmoke --console=plain` in the GraalVM Native Image matrix.
7. Upload available Gradle test, quality, and verification reports on failure.

## Native Image lane

Runtime primitive native tests start with `TASK-0010`, and generated-code smoke starts with `TASK-0013` through `:modules:generator-core:generatedCodeNativeSmoke`. `TASK-0020` promotes the native workflow from a placeholder to the mandatory representative native lane by running `./gradlew validateDesignControlPack nativeSmoke --console=plain` on the GraalVM Java 21 and Java 25 matrix.

The root `nativeSmoke` aggregate currently covers:

- `:modules:runtime-core:nativeTest`
- `:modules:runtime-jdkxml:nativeTest`
- `:modules:generator-core:generatedCodeNativeSmoke`, including representative choice and facet
  generated-code paths accepted for the `0.2.0` readiness evidence
- `:examples:purchase-order:nativeTest`
- `:examples:multi-namespace:nativeTest`
