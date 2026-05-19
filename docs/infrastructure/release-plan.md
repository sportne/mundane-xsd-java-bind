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

`TASK-0030` accepts additional `0.3.0` Composed XSD 1.0 readiness evidence for the opt-in
`XP-XSD10-COMPOSED` profile. That evidence documents accepted named model group and
attribute-group flattening, named list/union simple types, initial derivation flattening,
conformance/interop fixtures, and representative Native Image smoke paths; it still does not
authorize artifact publication or a `v0.1.0`/`v0.2.0`/`v0.3.0` release tag.

`TASK-0035` accepts additional `0.4.0` XSD 1.0 semantic expansion readiness evidence for the
opt-in `XP-XSD10-SEMANTIC` profile. That evidence documents accepted nillable/default/fixed
behavior, direct substitution-group behavior, generated semantic validation hardening,
conformance/interop fixtures, and representative Native Image smoke paths; it still does not
authorize artifact publication or a `v0.1.0`/`v0.2.0`/`v0.3.0`/`v0.4.0` release tag.

`TASK-0040` accepts additional `0.5.0` document-oriented/open-content readiness evidence for the
opt-in `XP-XSD10-DOCUMENT` profile. That evidence documents accepted direct wildcard/open-content
retention, accepted mixed-content sequence models, stable project serialization policy evidence,
conformance/interop fixtures, and representative generated-code smoke coverage; it still does not
authorize artifact publication, XML Canonicalization claims, full XSD 1.0 conformance claims, or a
`v0.1.0`/`v0.2.0`/`v0.3.0`/`v0.4.0`/`v0.5.0` release tag.

Public alpha/beta releases must state supported compatibility profiles and conformance status.

## `0.6.0` release maturity planning

`TASK-0041` plans hardening and release maturity only. It does not authorize artifact publication,
release workflow execution, signing, staging, or a release tag. `TASK-0045` owns release
engineering implementation.

Release readiness for `0.6.0` requires:

- supported profile statements that match the compatibility profile and conformance matrix;
- repeatable conformance/interop evidence from `TASK-0042`;
- documented benchmark baselines from `TASK-0043` without hard performance guarantees;
- selected Native Image conformance evidence from `TASK-0044`;
- local publication dry-run or equivalent artifact metadata validation in `TASK-0045`;
- release notes that name unsupported features and avoid full XSD 1.0, XSD 1.1, XML
  Canonicalization, cryptographic canonical XML, or untested performance claims.

## Release gate

No release may claim support for a schema feature unless the requirement, profile, conformance matrix, tests, and docs are complete.
