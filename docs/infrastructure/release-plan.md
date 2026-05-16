# Release plan

## Planned artifact coordinates

```text
:modules:xsdbind-bom              -> io.github.xsdbind:xsdbind-bom
:modules:runtime-core             -> io.github.xsdbind:xsdbind-runtime-core
:modules:runtime-jdkxml           -> io.github.xsdbind:xsdbind-runtime-jdkxml
:modules:generator-api            -> io.github.xsdbind:xsdbind-generator-api
:modules:generator-core           -> io.github.xsdbind:xsdbind-generator-core
:modules:generator-cli            -> io.github.xsdbind:xsdbind-cli
:modules:generator-gradle-plugin  -> io.github.xsdbind:xsdbind-gradle-plugin
:modules:testing-support          -> io.github.xsdbind:xsdbind-testing-support
```

## Versioning

Pre-implementation versions use `0.1.0-SNAPSHOT`. Public alpha/beta releases must state supported compatibility profiles and conformance status.

## Release gate

No release may claim support for a schema feature unless the requirement, profile, conformance matrix, tests, and docs are complete.
