# Naming collision review

`TASK-0075` reviewed generated naming and public package customization after `1.0.0`. The review
keeps customization limited to the existing default-package and namespace-package mappings.

| Collision class | Current behavior | Evidence |
|---|---|---|
| Duplicate type local names mapped into one Java package | Type names remain deterministic by suffixing later generated types (`Order`, `Order2`, ...), and helper names follow the suffixed model type. | `CoreGeneratorTest.namespaceMappingsCanPlaceDuplicateLocalTypeNamesInOnePackage` |
| Same-basename schemas in different local roots | Resource IDs include a bracketed shortest distinguishing local-root suffix when more than one local root is active, so `root[first]/order.xsd` and `root[second]/order.xsd` remain distinct while single-root diagnostics keep their previous relative IDs. | `SchemaResolverEdgeTest.disambiguatesSameBasenameSchemasAcrossMultipleLocalRoots`; `SchemaResolverEdgeTest.keepsRootPrefixSeparateFromSchemaRelativePath`; `CoreGeneratorTest.namespaceMappingsCanPlaceDuplicateLocalTypeNamesInOnePackage` |
| Duplicate root helper names for two root elements sharing one model type | Generation fails before writing partial output because reader/writer/validator helper names would collide. | `CoreGeneratorTest.duplicateRootHelperNamesReturnDiagnosticsWithoutWriting` |
| Java keyword element and attribute names | Field names are escaped with a leading underscore while preserving XML names in reader/writer metadata. | `CoreGeneratorTest.javaKeywordElementAndAttributeNamesAreEscapedDeterministically` |
| Invalid Java package customization | Public diagnostics keep the stable `SCHEMA_BINDING_INVALID_CONFIGURATION` code and now include Java package syntax guidance. | `CoreGeneratorTest.invalidBindingConfigurationWritesNoSources`; `MxjbGradlePluginFunctionalTest.missingSchemaAndInvalidPackageDiagnosticsFailWithoutPartialOutput` |
| CLI mapping syntax mistakes | CLI diagnostics keep `GENERATOR_CLI_INVALID_ARGUMENT` and point users to `ns=package` or `uri=path` mapping forms. | `MxjbCliTest.invalidMappingOptionsReturnActionableDiagnostics` |

`TASK-0078` adds the resource-ID fix without accepting new customization language. Remaining
high-value naming work is broader generated source stress coverage for dynamic `xsi:type` branch
naming, substitution branch naming, grouped content branch names, and retained wildcard content
names across larger schemas.
