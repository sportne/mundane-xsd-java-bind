# Release plan

## Planned artifact coordinates

```text
:modules:mxjb-bom              -> io.github.mundanej:mxjb-bom
:modules:runtime-core             -> io.github.mundanej:mxjb-runtime-core
:modules:runtime-jdkxml           -> io.github.mundanej:mxjb-runtime-jdkxml
:modules:generator-api            -> io.github.mundanej:mxjb-generator-api
:modules:generator-core           -> io.github.mundanej:mxjb-generator-core
:modules:generator-cli            -> io.github.mundanej:mxjb-cli
:modules:generator-gradle-plugin  -> io.github.mundanej:mxjb-gradle-plugin
:modules:testing-support          -> io.github.mundanej:mxjb-testing-support
```

## Versioning

The first public vertical slice remains `0.1.0-SNAPSHOT` readiness evidence. It proves the
supported `XP-DATA-10` generator path can be exercised through API, CLI, Gradle plugin, JVM checks,
representative round trips, and Native Image smoke tests, but it is not a publication-ready release.

Public alpha/beta releases must state supported compatibility profiles and conformance status.

## Release gate

No release may claim support for a schema feature unless the requirement, profile, conformance matrix, tests, and docs are complete.
