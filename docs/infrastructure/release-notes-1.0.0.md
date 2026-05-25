# 1.0.0 release notes

Status: `1.0.0` GitHub Release notes.

This release is for `mundane XSD Java Binding`, a Java 21 XML Schema 1.0 binding generator. It
generates immutable model records, XML readers, XML writers, and generated validators for the
project's accepted XML Schema 1.0 binding scope. It is not a standalone generic XML Schema
validator.

## Supported profiles

- `XP-DATA-10`
- `XP-DATA-10-CHOICE`
- `XP-VALIDATION-10-BASIC`
- `XP-XSD10-COMPOSED`
- `XP-XSD10-SEMANTIC`
- `XP-XSD10-DOCUMENT`
- `XP-XSD10-FULL`

## Evidence

- `TASK-0059` through `TASK-0065` close the full-XSD blocker sequence and enable
  `XP-XSD10-FULL`.
- Selected local conformance fixtures cover every executable profile family, including a
  full-profile generated-binding round trip.
- The pinned W3C XML Schema 1.0 suite intake is available through the opt-in
  `w3cXsd10Conformance` lane and records three mapped `AttrDecl` generated-binding rows plus
  classification for unmapped rows.
- `benchmarkSmoke` records advisory generated read/write/validate baselines.
- `nativeSmoke` and `nativeConformance` provide GraalVM Native Image evidence when `native-image` is
  available.
- `publicationDryRun -Pmxjb.version=1.0.0` stages the approved Maven-layout artifacts locally under
  `build/staging-repository`.

## Distribution

GitHub Release assets only:

- `mxjb-staging-repository-1.0.0.zip`: zipped Maven-layout staging repository.
- `artifact-manifest-1.0.0.txt`: deterministic list of staged files.
- `release-notes-1.0.0.md`: these release notes.
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
