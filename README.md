# mundane XSD Java Binding

`mundane XSD Java Binding` generates Java 21 model, XML reader, XML writer, and validator code from
XML Schema 1.0. Generated code is explicit, deterministic, reflection-free, and designed to work
well with GraalVM Native Image.

This is a schema-to-code project. It is not a code-to-schema tool, a standalone generic XML Schema
validator, or a JAXB-compatible runtime binding layer.

## Start here

- Generate from the command line: see `docs/getting-started.md`.
- Use the Gradle plugin: see `docs/getting-started.md`.
- Consume the GitHub Release Maven-layout asset: see `docs/build/release-consumption.md`.
- Check supported schema behavior: see `docs/compatibility-profiles.md` and
  `docs/verification/xsd10-full-feature-matrix.md`.

## Current scope

The current top-level profile is `XP-XSD10-FULL`: executable XML Schema 1.0 generated-binding
support for this project's accepted product scope. It composes the earlier data, choice,
validation, composed-schema, semantic, document/open-content, datatype, identity-constraint, and
generated-validation slices.

Evidence is intentionally narrower than a broad conformance claim. The feature matrix is the source
of truth for fully verified, partially verified, and explicitly mapped W3C evidence. Unmapped W3C
suite rows remain classified evidence, not generated-binding support claims.

## Non-goals

- XSD 1.1 and XML 1.1.
- Broad W3C XML Schema full-suite generated-binding support beyond explicitly mapped rows.
- XML Canonicalization, XML Signature canonical forms, lexical prefix preservation, comments/PI
  retention, DTD/entity identity preservation, or DOM-backed generated binding.
- Maven Central/package-registry publication, signing, or hard performance guarantees.

## Common project checks

```bash
./gradlew validateDesignControlPack qualityGate --console=plain
./gradlew :modules:generator-core:generatedCodeSmoke --console=plain
./gradlew :modules:conformance-tests:check --console=plain
```

Optional evidence lanes:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
./gradlew nativeSmoke nativeConformance --console=plain
./gradlew -Pmxjb.version=1.0.0 publicationDryRun --console=plain
./gradlew releaseConsumerSmoke --console=plain
```

`nativeSmoke` and `nativeConformance` require GraalVM `native-image`; this repository's local
evidence uses SDKMAN GraalVM. `releaseConsumerSmoke` validates the GitHub Release Maven-layout asset
path locally without publishing to Maven Central, signing artifacts, or contacting a package
registry.

## Repository map

- `modules/generator-api`: public generator API.
- `modules/generator-core`: schema compiler, binding planner, and generated source emitters.
- `modules/generator-cli`: command-line entry point.
- `modules/generator-gradle-plugin`: Gradle plugin.
- `modules/runtime-core`: dependency-free runtime interfaces and values used by generated code.
- `modules/runtime-jdkxml`: optional JDK XML adapter implementation.
- `modules/conformance-tests`: selected local conformance, interop, benchmark, and Native Image
  evidence lanes.
- `docs/`: requirements, architecture, verification, infrastructure, ADRs, and task handoff.
