# 1.0.1 release notes

Status: `1.0.1` GitHub Release notes.

This patch release keeps the `1.0.0` generated-binding product scope and distribution model. It
adds post-`1.0.0` hardening, evidence expansion, diagnostics, release-consumer validation, and
behavior-preserving architecture refactors. It is not a standalone generic XML Schema validator.

## Supported profiles

- `XP-DATA-10`
- `XP-DATA-10-CHOICE`
- `XP-VALIDATION-10-BASIC`
- `XP-XSD10-COMPOSED`
- `XP-XSD10-SEMANTIC`
- `XP-XSD10-DOCUMENT`
- `XP-XSD10-FULL`

## Changes since 1.0.0

- Fixed schema resource identity for same-basename schemas in different directories.
- Hardened remaining conformance `SchemaFactory` helper use against external DTD/schema access.
- Expanded mapped W3C XML Schema 1.0 generated-binding evidence to nine rows and three executable
  mapped binding paths while keeping broad full-suite generated-binding support unclaimed.
- Added downstream `releaseConsumerSmoke` validation for GitHub Release Maven-layout assets.
- Improved generated naming, diagnostics, XML security posture, and advisory performance evidence.
- Added generator/runtime architecture refactors for IR normalization, binding planning, emitter
  planning, W3C execution separation, and runtime datatype helper families without public API or
  dependency expansion.
- Fixed a `base64Binary` lexical-validation hole by stripping XML whitespace before strict Base64
  decoding.

## Evidence

- `TASK-0067` through `TASK-0093` record post-`1.0.0` claim reconciliation, hardening,
  conformance/evidence expansion, performance characterization, release-consumer smoke evidence,
  documentation simplification, and architecture refactors.
- `publicationDryRun -Pmxjb.version=1.0.1` stages the approved Maven-layout artifacts locally under
  `build/staging-repository`.
- `releaseConsumerSmoke` validates the staged GitHub Release asset shape from a clean downstream
  Gradle project without remote publication, signing, or package-registry access.
- `nativeSmoke` and `nativeConformance` remain optional SDKMAN GraalVM evidence lanes when
  `native-image` is available.

## Distribution

GitHub Release assets only:

- `mxjb-staging-repository-1.0.1.zip`: zipped Maven-layout staging repository.
- `artifact-manifest-1.0.1.txt`: deterministic list of staged files.
- `release-notes-1.0.1.md`: these release notes.
- `SHA256SUMS`: checksums for the uploaded assets.

## Explicit non-claims

- No Maven Central publication is performed.
- No package registry publication is performed.
- No signing keys, credentials, or remote staging repositories are configured.
- XSD 1.1 and XML 1.1 support are not claimed and are not project targets.
- XML Canonicalization is not claimed.
- XML Signature canonical forms, lexical prefix preservation, comments, processing instructions,
  DTD retention, and entity-reference identity are not claimed.
- DOM-backed binding and code-to-schema generation are not supported.
- Broad W3C full-suite generated-binding coverage is not claimed beyond explicitly mapped rows.
- Benchmark output is advisory evidence, not a hard performance guarantee.

## Rollback

If the GitHub Release workflow fails, do not publish alternate artifacts manually. Delete any failed
draft release or local `build/staging-repository` contents, fix the repository, rerun the local
gates, and cut a reviewed follow-up before creating another release tag.
