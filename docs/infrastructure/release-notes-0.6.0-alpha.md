# 0.6.0-alpha.0 release dry-run notes

Status: release-engineering dry-run evidence only; this is not a released artifact.

`TASK-0045` validates local staging and metadata for a candidate `0.6.0-alpha.0` publication with:

```bash
./gradlew -Pmxjb.version=0.6.0-alpha.0 publicationDryRun --console=plain
```

The repository default remains `0.1.0-SNAPSHOT` in `gradle.properties`. The candidate version is
passed on the command line so readiness evidence does not commit a version bump.

## Supported profile statement

The dry-run candidate describes the accepted local evidence for these opt-in compatibility profiles:

- `XP-DATA-10`
- `XP-DATA-10-CHOICE`
- `XP-VALIDATION-10-BASIC`
- `XP-XSD10-COMPOSED`
- `XP-XSD10-SEMANTIC`
- `XP-XSD10-DOCUMENT`

Evidence remains selected and local. Full XSD 1.0 conformance is not claimed.

## Evidence summary

- `TASK-0042` records selected local conformance and interop fixture classification.
- `TASK-0043` records advisory benchmark smoke output for generated read, write, validate, and
  document open-content workloads.
- `TASK-0044` records the selected Native Image conformance lane and the local `native-image`
  toolchain blocker when GraalVM Native Image is unavailable.
- `TASK-0045` stages the BOM, runtime modules, generator modules, Gradle plugin implementation,
  Gradle plugin marker, and testing support artifacts to `build/staging-repository`.
- `TASK-0046` reconciles final `0.6.0` readiness evidence across public docs, conformance,
  benchmarks, Native Image, release dry-run, security posture, and traceability.

## Explicit non-claims

- No remote publication is performed.
- No release tag is created.
- No signing keys, credentials, or remote staging repositories are configured.
- Full XSD 1.0 conformance is not claimed.
- XSD 1.1 and XML 1.1 support are not claimed and are not project targets.
- XML Canonicalization is not claimed.
- XML Signature canonical forms, lexical prefix preservation, comments, processing instructions,
  DTD retention, and entity-reference identity are not claimed.
- Benchmark output is advisory evidence, not a hard performance guarantee.
- Native Image conformance depends on a local GraalVM `native-image` toolchain.

## Unsupported features still called out

Unsupported feature claims remain explicit for `xs:anyAttribute`, wildcard `processContents="lax"`
or `"strict"`, unsupported wildcard namespace constraints, wildcard choices, mixed choices,
complex mixed derivation edge cases, identity constraints, full derivation semantics,
DOM-backed binding, parser-handle retention, comments/PI preservation, DTD/entity
identity, XSD 1.1, and XML 1.1.

## Rollback

The dry-run is local. Rollback consists of deleting `build/staging-repository` and reverting the
`TASK-0045` build/documentation changes. Do not push tags, publish artifacts, or upload staged files
unless a later task explicitly authorizes a real release.
