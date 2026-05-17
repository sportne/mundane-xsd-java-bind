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

`TASK-0025` accepts additional `0.2.0` Practical Data Contracts readiness evidence for the opt-in
`XP-DATA-10-CHOICE` and `XP-VALIDATION-10-BASIC` profiles. That evidence documents implemented
choice and facet subsets, conformance/interop fixtures, and representative Native Image smoke paths;
it still does not authorize artifact publication or a `v0.1.0`/`v0.2.0` release tag.

Public alpha/beta releases must state supported compatibility profiles and conformance status.

## Release gate

No release may claim support for a schema feature unless the requirement, profile, conformance matrix, tests, and docs are complete.
