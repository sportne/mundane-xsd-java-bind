# mundane XSD Java Binding

`mundane XSD Java Binding` generates Java 21 model, XML reader, XML writer, and validator code from
selected XML Schema 1.0 profiles. Generated code is explicit, deterministic, reflection-free, and
designed to work well with GraalVM Native Image.

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
  types, and initial derivation flattening.
- `XP-XSD10-SEMANTIC`: accepted `nillable`, scalar `default`, scalar `fixed`, direct substitution
  groups, and expanded generated validation for those paths.
- `XP-XSD10-DOCUMENT`: accepted direct `xs:any` wildcard/open-content retention, accepted
  `mixed="true"` sequence content, retained `XmlFragment` values, and stable project serialization
  policy.

Across the executable profiles, accepted scalar element and attribute positions use the shared XSD
1.0 datatype engine: string, numeric, float/double, temporal, duration, binary, anyURI,
QName/NOTATION, list-valued built-ins, and restriction facets are supported for the schema shapes
the profiles already accept.

The planned full XML Schema 1.0 target is `XP-XSD10-FULL`. The public profile token exists for
planning, but generation intentionally rejects it until the follow-on implementation tasks complete.
The full feature matrix is in `docs/verification/xsd10-full-feature-matrix.md`.

## Not supported

- Full XML Schema 1.0 conformance is not claimed yet.
- Full content-model automata, full derivation/polymorphism, full attribute/wildcard algebra, and
  identity constraints are still planned work.
- XSD 1.1 and XML 1.1 are not project targets.
- XML Canonicalization, XML Signature canonical forms, lexical prefix preservation, comments/PI
  retention, DTD/entity identity preservation, and DOM-backed binding are not supported.
- Real artifact publication, signing, release tags, and hard performance guarantees are not claimed
  by this repository state.

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
./gradlew -Pmxjb.version=0.6.0-alpha.0 publicationDryRun
```

`nativeSmoke` and `nativeConformance` require `native-image` on `PATH`.

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
