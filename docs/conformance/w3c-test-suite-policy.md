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

`TASK-0041` keeps `0.6.0` planning in the same posture: `TASK-0042` may add selected local fixture
copies or documented intake scripts only after each selected test is mapped to a declared profile,
unsupported diagnostic, or future-study category. It must not vendor a broad W3C suite snapshot or
claim full-suite pass status.
