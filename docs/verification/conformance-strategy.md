# Conformance strategy

## Goals

- Avoid claiming broad XML Schema conformance before it exists.
- Map every supported feature to a profile and test set.
- Use W3C XML and XML Schema suites as reference material where practical.
- Separate supported, unsupported-by-design, future, and blocked statuses.

## Conformance status values

```text
not-started | designed | implemented | partially-supported | unsupported-by-design | future | blocked
```

## Test selection rule

A W3C or external conformance test is included only when:

- it maps to a declared profile, or
- it is used to prove unsupported-feature diagnostics, or
- it is part of a future-profile readiness study.

## `0.6.0` hardening intake plan

`TASK-0041` defines the planning scope for hardening conformance and interop evidence without
claiming full XML Schema conformance. `TASK-0042` implements the first selected local fixture
manifest for this lane under `modules/conformance-tests/src/test/resources/selected-fixtures.tsv`.

Selected W3C or external fixtures must be classified before they are executable evidence:

- `supported-profile`: the fixture maps to one declared compatibility profile and expected
  behavior is already implemented.
- `unsupported-diagnostic`: the fixture proves deterministic rejection for an unsupported schema or
  XML construct.
- `future-study`: the fixture informs a future profile but does not affect current support claims.
- `blocked`: the fixture cannot run locally because of licensing, toolchain, storage, or dependency
  constraints; the blocker must be documented.

`docs/verification/external-issue-regression-mining.md` records post-1.0.0 issue-mining intake from
adjacent XML Schema binding/codegen projects. Those sources can justify local regression fixtures
only when the behavior is already claimed, or follow-on task candidates when the behavior is outside
the current product scope.

Interop comparisons must be repeatable from local inputs. Accepted comparison targets are JDK XML
Schema validation, secure JDK XML parsing, generated binding round trips, and documented semantic
comparisons. Byte-identical XML output, W3C XML Canonicalization, XML Signature canonical forms,
networked test retrieval, and broad vendoring of external suites remain out of scope unless a later
task and ADR approve them.

The `TASK-0042` manifest covers the current supported profile families (`XP-DATA-10`,
`XP-DATA-10-CHOICE`, `XP-VALIDATION-10-BASIC`, `XP-XSD10-COMPOSED`,
`XP-XSD10-SEMANTIC`, and `XP-XSD10-DOCUMENT`) and selected unsupported-diagnostic schemas. Blocked
and future-study rows are classification evidence only; they do not create runnable suite coverage.

## Full XSD 1.0 suite intake

`TASK-0055` adds the opt-in `w3cXsd10Conformance` lane for the pinned W3C XML Schema 1.0
2007-06-20 archive. Normal checks do not download or vendor the suite; callers provide a local
`xmlschema2006-11-06` directory with `-Pmxjb.w3cXsd10SuiteDir`.

The accepted full-suite intake classifies every discovered W3C schema or instance document into one
category: `binding-supported`, `validation-only`, `tolerated-metadata`, `expected-diagnostic`,
`product-scope-incompatible`, or `blocked`. `TASK-0064` adds the first explicit generated-binding
mapping for the selected `AttrDecl` row subset. The current pinned-suite evidence is:

```text
w3c-xsd10-summary total=24796 binding-supported=3 validation-only=24436 tolerated-metadata=98 expected-diagnostic=2 product-scope-incompatible=167 blocked=90
w3c-xsd10-binding-execution passed=1
```

The `binding-supported` count is limited to explicitly mapped rows. Unmapped rows remain
repeatable classification and diagnostic evidence, not a full XSD 1.0 support claim.

`TASK-0056` accepted this as the final readiness reconciliation for the earlier sequence.
`TASK-0065` enables `XP-XSD10-FULL`, and `TASK-0066` accepts the `1.0.0` GitHub Release workflow and
public release claim for the generated-binding scope.

## Unsupported feature behavior

Unsupported schema features must produce explicit diagnostics. Silent partial interpretation is forbidden.
