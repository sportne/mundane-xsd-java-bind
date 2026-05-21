# TASK-0055: xsd10-full-conformance-suite

Status: draft.

Task ID: `TASK-0055`
Gate: pinned XSD 1.0 conformance suite intake.
Target areas: conformance-tests, fixture manifests, W3C suite policy, verification docs, CI docs
Allowed files: conformance test harness, local selected fixtures or pinned metadata, docs
Forbidden files: networked test retrieval in normal gates, broad vendoring without storage/license review, product behavior changes, XSD 1.1/XML 1.1
Expected behavior: add a repeatable W3C XML Schema 1.0 suite intake process with fixture classification for binding-supported, validation-only, tolerated metadata, expected diagnostic, and incompatible product-scope cases.
Tests to add/update: manifest parser tests, suite classification tests, generated-binding/JDK comparison tests for accepted fixtures.
Acceptance criteria: the conformance suite can be run locally from pinned inputs and produces stable summarized evidence without claiming unsupported scope.
Rollback notes: remove suite intake wiring and generated classifications from this task.
