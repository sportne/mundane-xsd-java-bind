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
- documented benchmark baselines from `TASK-0043`, produced by `./gradlew benchmarkSmoke`, without
  hard performance guarantees;
- selected Native Image conformance evidence from `TASK-0044`;
- local publication dry-run or equivalent artifact metadata validation in `TASK-0045`;
- release notes that name unsupported features and avoid full XSD 1.0, XSD 1.1, XML 1.1, XML
  Canonicalization, cryptographic canonical XML, or untested performance claims.

`TASK-0045` adds the local release-engineering dry-run lane:

```bash
./gradlew -Pmxjb.version=0.6.0-alpha.0 publicationDryRun --console=plain
```

The lane stages only the approved publishable coordinates to `build/staging-repository`: the BOM,
runtime modules, generator API/core/CLI/Gradle plugin, testing support, and the Gradle plugin
marker. It validates expected artifact files, rejects unexpected publication coordinates such as
internal conformance/example/build-logic projects, checks staged POM/module metadata for local path
or secret-like leakage, and confirms the release notes preserve non-claims. The repository default
version remains `0.1.0-SNAPSHOT`; candidate validation uses `-Pmxjb.version=0.6.0-alpha.0` and does
not create a release tag, signing setup, credentials, remote staging repository, or actual
publication.

`TASK-0046` closes the `0.6.0` hardening slice as readiness evidence only. It confirms selected
conformance/interop, advisory benchmark, Native Image conformance, and publication dry-run evidence
agree with public documentation. It does not convert `0.6.0-alpha.0` into a released artifact,
authorize signing or remote staging, create a release tag, or broaden supported schema claims.

`TASK-0072` adds downstream release-asset consumption evidence:

```bash
./gradlew releaseConsumerSmoke --console=plain
```

The lane depends on `publicationDryRun`, zips and unpacks the Maven-layout staging repository like
the GitHub Release asset, creates a clean temporary Gradle consumer project, verifies a missing local
repository path fails with a clear diagnostic, and then runs generated source compilation plus
generated read/write/validate offline from the unpacked asset repository. It does not publish
remotely, sign artifacts, retag a release, or use Maven Central/package registries.

`TASK-0056` closed the earlier `XP-XSD10-FULL` readiness sequence with a negative broad-support
decision. `TASK-0064` maps the first three W3C rows to generated-binding execution, `TASK-0065`
enables the profile for accepted product-scope generated-binding shapes, and `TASK-0066` accepts
final readiness, release notes, version metadata, and the GitHub Release workflow.

## `1.0.0` full-XSD release sequence

`TASK-0058` defines the `1.0.0` release bar as executable `XP-XSD10-FULL` generated-binding support
for accepted product-scope shapes plus explicit W3C generated-binding evidence. A stable-subset
release is not sufficient for `1.0.0`.

Before any `1.0.0` release workflow, version update, tag, or GitHub Release artifact upload, the
following gates had to be accepted:

- `TASK-0059`: grouped content-list models for remaining full-XSD content shapes.
- `TASK-0060`: complete content-model automata and UPA validation.
- `TASK-0061`: complete derivation, restriction, block/final, and `xsi:type` behavior.
- `TASK-0062`: strict/lax wildcard deep validation and wildcard composition.
- `TASK-0063`: remaining datatype, nil, and identity-validation edges.
- `TASK-0064`: W3C XML Schema 1.0 generated-binding row mapping and executable evidence.
- `TASK-0065`: executable `XP-XSD10-FULL` profile enablement.
- `TASK-0066`: final readiness reconciliation, version metadata, release notes, and GitHub Release
  workflow.

The `TASK-0066` release workflow must publish only GitHub Release assets from `v1.0.0` tags:
a zipped Maven-layout `build/staging-repository`, checksums, release notes, and an artifact
manifest. It must not publish to Maven Central or package registries, require signing, or introduce
release secrets.

## `1.0.1` patch release

`TASK-0094` authorizes a `1.0.1` patch release from the accepted post-`1.0.0` hardening and
architecture work. The release keeps the `1.0.0` public product scope and GitHub Release
asset-only distribution model.

Before the `v1.0.1` tag, the release task must:

- update version metadata and `docs/infrastructure/release-notes-1.0.1.md`;
- keep release notes and public docs within the existing `XP-XSD10-FULL` generated-binding product
  scope and explicit non-claims;
- validate the local Maven-layout asset with `publicationDryRun -Pmxjb.version=1.0.1`;
- validate clean downstream asset consumption with `releaseConsumerSmoke`;
- run the full quality gate and optional SDKMAN GraalVM Native Image evidence lane where available;
- make the release workflow version-aware for `v*.*.*` tags while preserving GitHub Release assets
  only;
- reject non-strict release-version tags and release tags that do not point at `origin/main`.

## Release gate

No release may claim support for a schema feature unless the requirement, profile, conformance matrix, tests, and docs are complete.
