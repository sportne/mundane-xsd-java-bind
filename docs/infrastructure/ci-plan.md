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
6. Run `./gradlew validateDesignControlPack nativeSmoke nativeConformance --console=plain` in the GraalVM Native Image matrix.
7. Upload available Gradle test, quality, and verification reports on failure.

## Native Image lane

Runtime primitive native tests start with `TASK-0010`, and generated-code smoke starts with
`TASK-0013` through `:modules:generator-core:generatedCodeNativeSmoke`. `TASK-0020` promotes the
native workflow from a placeholder to the mandatory representative native lane by running
`./gradlew validateDesignControlPack nativeSmoke --console=plain` on the GraalVM Java 21 and Java
25 matrix. `TASK-0044` keeps that smoke aggregate intact and adds
`./gradlew validateDesignControlPack nativeSmoke nativeConformance --console=plain` for selected
conformance execution in the same GraalVM workflow.

The root `nativeSmoke` aggregate currently covers:

- `:modules:runtime-core:nativeTest`
- `:modules:runtime-jdkxml:nativeTest`
- `:modules:generator-core:generatedCodeNativeSmoke`, including representative choice, facet,
  composed, semantic, substitution, and document serialization generated-code paths accepted by
  their respective task evidence
- `:examples:purchase-order:nativeTest`
- `:examples:multi-namespace:nativeTest`

The root `nativeConformance` aggregate currently covers:

- `:modules:conformance-tests:nativeConformance`, a selected fixture executable that runs static
  generated read/write/validate paths across the supported profile families, selected
  unsupported-diagnostic schemas, and secure entity/resource denial.

## `0.6.0` planned hardening lanes

`TASK-0041` defines planned CI lanes for later tasks without changing active gates:

- `TASK-0042` should document repeatable conformance/interop commands and decide which, if any,
  become CI jobs.
- `TASK-0043` adds the explicit `./gradlew benchmarkSmoke --console=plain` lane and keeps it
  outside default `qualityGate`; it may run in scheduled CI after baselines are stable.
- `TASK-0044` adds selected Native Image conformance execution beside `nativeSmoke`, without
  weakening existing smoke checks.
- `TASK-0045` adds `./gradlew -Pmxjb.version=0.6.0-alpha.0 publicationDryRun --console=plain` as
  an explicit release-engineering dry-run lane. It validates local staging metadata only and must
  not publish from normal CI, sign artifacts, create tags, or require release secrets.
- `TASK-0055` adds `./gradlew -Pmxjb.w3cXsd10SuiteDir=/path/to/xmlschema2006-11-06 w3cXsd10Conformance --console=plain`
  as an explicit external-suite classification lane. It requires a pre-provisioned local W3C XML
  Schema 1.0 suite checkout and remains outside `qualityGate`.

`qualityGate` remains the required JVM-focused gate, `benchmarkSmoke` is advisory and opt-in, and
`nativeSmoke` plus `nativeConformance` remain GraalVM-only lanes. `publicationDryRun` and
`w3cXsd10Conformance` are opt-in evidence lanes and are not wired into `qualityGate`.

`TASK-0046` confirms the hardening lanes above are evidence lanes, not new required default gates.
Future CI changes for real publication, broader external suites, or benchmark thresholds require a
new accepted task and must not be inferred from the `0.6.0` readiness closeout.

## `1.0.0` release workflow plan

`TASK-0058` defines the `1.0.0` release bar as executable `XP-XSD10-FULL` generated-binding support
with W3C generated-binding evidence. The CI/release sequence remains planned until `TASK-0066`:

- `qualityGate` remains the JVM correctness gate throughout `TASK-0059` through `TASK-0065`.
- `TASK-0064` maps the first W3C generated-binding rows. `w3cXsd10Conformance` becomes a final
  release-blocking evidence lane in `TASK-0066`.
- `TASK-0065` enables executable `XP-XSD10-FULL`.
- The GitHub Release workflow is added only in `TASK-0066`, after full-XSD evidence passes.

The planned release workflow should trigger on `v1.0.0` tags, run final validation, execute
`publicationDryRun` with `-Pmxjb.version=1.0.0`, zip `build/staging-repository`, create checksums
and an artifact manifest, and upload those files to GitHub Release assets. It must not publish to
Maven Central or package registries, add signing, or require release secrets beyond the default
GitHub token.
