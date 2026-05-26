# Getting started

This page shows the shortest local path for generating Java bindings from an XSD. It is usage
guidance only; evidence and release-history details live in the verification and infrastructure
docs.

## From the repository checkout

Run the CLI against the purchase-order example schema:

```bash
./gradlew :modules:generator-cli:run --args="generate --schema ${PWD}/examples/purchase-order/src/main/resources/schema/purchase-order.xsd --output ${PWD}/build/generated/mxjb-getting-started"
```

The output directory contains generated Java model, reader, writer, and validator sources. Generated
sources depend on the project runtime modules rather than reflection or runtime classpath scanning.

## In a Gradle build

For a released consumer build, first configure plugin resolution from
`docs/build/release-consumption.md`. Then apply the plugin and declare explicit schema inputs:

```groovy
plugins {
    id 'java'
    id 'io.github.mundanej.mxjb' version '1.0.1'
}

mxjb {
    schema('src/main/resources/schema/order.xsd')
    namespacePackage('urn:orders', 'com.example.orders')
    profile = 'XP-XSD10-FULL'
}
```

Then run:

```bash
./gradlew generateMxjbSources
```

Use `namespacePackage` mappings for stable public package names. The generator reports unsupported
schema constructs with deterministic diagnostics rather than silently widening the supported scope.

## What to read next

- `docs/compatibility-profiles.md` summarizes supported profiles and non-goals.
- `docs/architecture/generated-code-contract.md` describes generated source shape.
- `docs/build/release-consumption.md` explains how to use the GitHub Release Maven-layout asset.
- `docs/verification/xsd10-full-feature-matrix.md` records detailed feature evidence.
