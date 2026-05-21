# W3C test-suite policy

W3C XML and XML Schema test suites are reference material for conformance strategy, but passing the full suite is not a phase-one goal.

## Use policy

- Use suite metadata to classify tests by feature/profile.
- Include only tests mapped to current or future profile goals.
- Do not claim full XSD 1.0 or XSD 1.1 conformance until the matrix supports that claim.
- Unsupported-feature tests should validate explicit diagnostics.

## Storage policy

Large external test suites should be pulled through documented scripts or Git submodules only after license and maintenance review. Do not vendor large suites in this design-control pack.

`TASK-0017` uses small local `XP-DATA-10` fixtures under `modules/conformance-tests` rather than
vendoring W3C suites. Those fixtures are representative conformance evidence for implemented
features only.

`TASK-0041` keeps `0.6.0` planning in the same posture. `TASK-0042` adds
`modules/conformance-tests/src/test/resources/selected-fixtures.tsv` as selected local fixture
classification evidence. The manifest maps existing local fixtures to declared profiles, adds
minimal local unsupported-diagnostic schemas, and records future-study/blocked rows without
vendoring a broad W3C suite snapshot or claiming full-suite pass status.

`TASK-0046` confirms the `0.6.0` closeout remains selected local evidence only. A future task that
wants broader W3C suite intake must define license review, storage policy, fixture selection,
profile mapping, expected diagnostics, and CI cost before adding or claiming broader coverage.
