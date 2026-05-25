# mundane XSD Java Binding

`mundane XSD Java Binding` generates Java 21 model, XML reader, XML writer, and validator code from
XML Schema 1.0. Generated code is explicit, deterministic, reflection-free, and designed to work
well with GraalVM Native Image.

This is a schema-to-code project. It is not a code-to-schema tool and it is not a general-purpose
XML Schema validator independent of generated bindings.

## Supported today

The current executable profiles are:

- `XP-DATA-10`: elements, complex types, attributes, nested elements, sequences, optional/repeated
  elements, namespaces, include/import, generated read/write/validate, CLI, Gradle plugin, and
  representative round trips.
- `XP-DATA-10-CHOICE`: accepted local singleton `xs:choice` particles.
- `XP-VALIDATION-10-BASIC`: accepted named simple restrictions for enumeration, string length,
  numeric inclusive ranges, and string pattern facets.
- `XP-XSD10-COMPOSED`: accepted named model groups, attribute groups, named list/union simple
  types including anonymous simple restriction list/union members, and initial derivation
  flattening.
- `XP-XSD10-SEMANTIC`: accepted `nillable`, scalar `default`, scalar `fixed`, direct substitution
  groups, `xs:unique`/`xs:key`/`xs:keyref` identity constraints including nilled-field handling
  for accepted generated model shapes, and expanded generated validation for those paths.
- `XP-XSD10-DOCUMENT`: accepted `xs:any` wildcard/open-content retention, accepted
  `xs:anyAttribute` retention, accepted wildcard choices, accepted `mixed="true"` sequence and
  mixed choice content, retained `XmlFragment` and `XmlAttribute` values, strict/lax
  schema-known wildcard validation for accepted retained declarations, and stable project
  serialization policy.
- `XP-XSD10-FULL`: executable XML Schema 1.0 generated-binding profile for the project's accepted
  product scope. It runs the same generated-binding pipeline, uses the evidence limits in the
  feature and conformance matrices, and retains the explicit non-goals below.

Across the executable profiles, accepted scalar element and attribute positions use the shared XSD
1.0 datatype engine: string, numeric, float/double, temporal, duration, binary, anyURI,
QName/NOTATION, list-valued built-ins, and restriction facets are supported for the schema shapes
the profiles already accept.

Content-model coverage now includes required `xs:all` groups, all-optional `xs:all` groups,
optional all-groups with required children, repeated element-only choices, repeated/optional
multi-particle groups, nested singleton sequences, single-particle repeated/optional group refs,
mixed choices, wildcard choices, and shared grouped-position reader/validator checks in the
currently executable profiles.

The full feature matrix is in `docs/verification/xsd10-full-feature-matrix.md`. It is the source of
truth for which areas are fully verified, partially verified, or limited to explicitly mapped W3C
rows.

## Not supported

- This is not a standalone generic XML Schema validator; validation is generated for binding models.
- Broad W3C suite generated-binding coverage beyond explicitly mapped rows is not claimed.
- W3C XML Schema 1.0 suite classification exists as opt-in evidence; three `AttrDecl` rows are
  mapped to generated-binding support.
- XSD 1.1 and XML 1.1 are not project targets.
- XML Canonicalization, XML Signature canonical forms, lexical prefix preservation, comments/PI
  retention, DTD/entity identity preservation, and DOM-backed binding are not supported.
- Maven Central/package-registry publication, signing, and hard performance guarantees are not
  claimed.

## Common commands

```bash
./gradlew help
./gradlew validateDesignControlPack
./gradlew qualityGate
./gradlew :modules:generator-core:generatedCodeSmoke
./gradlew :modules:conformance-tests:check
```

Native Image and publication dry-run lanes are explicit opt-in evidence commands:

```bash
./gradlew nativeSmoke
./gradlew nativeConformance
./gradlew -Pmxjb.version=1.0.0 publicationDryRun
./gradlew releaseConsumerSmoke
```

`nativeSmoke` and `nativeConformance` require `native-image` on `PATH`.
`releaseConsumerSmoke` validates the GitHub Release Maven-layout asset path locally; it does not
publish to Maven Central, sign artifacts, or contact a package registry.

## CLI example

```bash
./gradlew :modules:generator-cli:run --args="generate --schema ${PWD}/examples/purchase-order/src/main/resources/schema/purchase-order.xsd --output ${PWD}/build/generated/mxjb-readme"
```

## Gradle plugin example

```groovy
plugins {
    id 'java'
    id 'io.github.mundanej.mxjb'
}

mxjb {
    schema('src/main/resources/schema/order.xsd')
    namespacePackage('urn:orders', 'com.example.orders')
}
```

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
