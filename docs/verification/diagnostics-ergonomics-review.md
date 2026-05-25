# Diagnostics ergonomics review

`TASK-0074` reviewed common public failure paths and keeps diagnostic output deterministic: every
public generator failure still renders as `CODE | resource | message`, and XML runtime validation
continues to return stable generated error codes.

| Surface | Common failure | Stable evidence | Expected next action |
|---|---|---|---|
| Public API/core | Null request, missing schemas, or missing output directory | `CoreGeneratorTest.requestValidationDiagnosticsIncludeNextActions`; `CoreGeneratorTest.nullRequestDiagnosticIncludesNextAction` | Create a `GeneratorRequest`, add at least one schema path, and set the generated-source output directory. |
| CLI | Missing option values, invalid profile tokens, bad namespace/package or catalog syntax | `MxjbCliTest` invalid-option coverage | Run `mxjb --help`, choose a supported profile token, and use `ns=package` or `uri=path` mappings. |
| CLI/API resolver | Missing schema file, denied network schemaLocation, path outside local roots | `MxjbCliTest.missingSchemaFileReturnsActionableResourceDiagnostic`; resolver tests | Check the schema path, add a catalog mapping, or add an explicit local root. |
| Gradle plugin | Invalid profile, invalid catalog URI, missing schema configuration, generator diagnostics surfaced through TestKit | `MxjbGradlePluginUnitTest`; `MxjbGradlePluginFunctionalTest` | Set `mxjb.schema(...)`, choose a supported profile, configure catalog keys as URIs/schemaLocation values, and read the manifest-line diagnostic. |
| W3C suite lane | Missing or wrong suite directory | `W3cXsd10SuiteIntakeTest` suite-root diagnostics | Download/extract the pinned suite and pass the `xmlschema2006-11-06` directory. |
| Release asset consumption | Wrong local release-asset repository path | `releaseConsumerSmoke` | Point the consumer build at the unpacked Maven-layout release asset repository. |
| Generated XML validation | Invalid XML or invalid object values | generated validator/conformance tests | Inspect `MXJB-GR-*`, `MXJB-DT-*`, and `MXJB-GV-*` codes with available XML locations. |

The review does not add new schema features, new dependencies, release metadata, or alternate output
formats. Absolute paths remain limited to user-provided paths or build-tool context where the user
needs the path to fix local configuration.
