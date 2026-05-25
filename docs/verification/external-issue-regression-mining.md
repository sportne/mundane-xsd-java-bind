# External XML binding issue mining

## TASK-0068 intake

`TASK-0068` reviewed recurring failure modes from adjacent XML Schema binding/codegen projects and
classified each theme against the current `mundane XSD Java Binding` evidence. This note is an
intake record, not a support expansion.

Reviewed sources:

- [JAXB RI releases](https://github.com/eclipse-ee4j/jaxb-ri/releases), including fixes for XJC
  optional-property null pointers, boolean lexical parsing, package-name derivation, generated
  episode output, systemId resolution regressions, OSGi class loading, and test reflection cleanup.
- [jaxb-tools releases](https://github.com/highsource/jaxb-tools/releases), including Maven 4 build
  issues, configurable maximum identifier length, custom naming strategy examples, classpath
  exclusions, dependency removal, and JDK 25 build coverage.
- [jaxb-tools issue 204](https://github.com/highsource/jaxb-tools/issues/204), a JDK 17 reflection
  crash report.
- [xsdata issue 564](https://github.com/tefra/xsdata/issues/564), covering user confusion around
  remote includes, custom simple types, repeated nested choices, and silent missing generated
  fields.
- [xsdata README](https://github.com/tefra/xsdata), covering XML Schema 1.0/1.1 generation,
  wildcard support, XInclude, unknown properties, and W3C suite claims.
- [XmlSchemaClassGenerator README](https://github.com/mganss/XmlSchemaClassGenerator), covering
  nillable/nullable handling, namespace mapping, type/member substitution, choice behavior, DTD
  parsing defaults, and known unsupported cases such as recursive choices and name clashes.
- [XmlSchemaClassGenerator issue 256](https://github.com/mganss/XmlSchemaClassGenerator/issues/256),
  covering setup/tooling confusion when an installed generator executable is not available on build
  machine `PATH`.

## Disposition table

| Theme | Source links | Current evidence and disposition |
|---|---|---|
| Choice ordering and list shape | [xsdata issue 564](https://github.com/tefra/xsdata/issues/564), [XmlSchemaClassGenerator README](https://github.com/mganss/XmlSchemaClassGenerator) | Existing evidence: `XpData10ChoiceConformanceTest`, `XpXsd10DocumentConformanceTest`, `GeneratedReaderEmitterTest`, and `GeneratedValidatorEmitterTest` cover accepted singleton, repeated element-only, mixed, wildcard, and grouped choice paths. `TASK-0070` should expand W3C row evidence for these shapes; no support change in this task. |
| Wildcard ambiguity | [xsdata README](https://github.com/tefra/xsdata), [XmlSchemaClassGenerator README](https://github.com/mganss/XmlSchemaClassGenerator) | Existing evidence: `XpXsd10DocumentConformanceTest`, `SchemaIrBuilderTest`, `GeneratedValidatorEmitterTest`, and fixture `T-CONF-XP-XSD10-DOCUMENT-WILDCARD-DEEP` cover accepted retained element/attribute wildcards, namespace tokens, schema-known strict/lax validation, and UPA diagnostics. `TASK-0073` should recheck security boundaries for retained wildcard fragments. |
| Substitution, abstract dispatch, and `xsi:type` | [JAXB RI releases](https://github.com/eclipse-ee4j/jaxb-ri/releases), [xsdata README](https://github.com/tefra/xsdata) | Existing evidence: `XpXsd10SemanticConformanceTest`, `BindingModelBuilderTest`, fixture `T-CONF-XP-XSD10-SEMANTIC-SUBSTITUTION-REPEATED`, and fixture `T-CONF-XP-XSD10-SEMANTIC-XSI-TYPE` cover accepted substitution branches and known declared-base `xsi:type` branches. Direct root-element `xsi:type` dispatch and broader W3C mapping remain follow-up candidates. |
| Defaults, nullability, and nil | [JAXB RI releases](https://github.com/eclipse-ee4j/jaxb-ri/releases), [XmlSchemaClassGenerator README](https://github.com/mganss/XmlSchemaClassGenerator) | Existing evidence: `XpXsd10SemanticConformanceTest`, `GeneratedValidatorEmitterTest`, and fixture `T-CONF-XP-XSD10-SEMANTIC-IDENTITY` cover accepted scalar defaults/fixed values, required singleton nillable elements, nil-aware identity fields, and deterministic unsupported diagnostics for out-of-scope nil/default shapes. `TASK-0074` should ensure diagnostics remain actionable. |
| Security and XXE/DTD defaults | [XmlSchemaClassGenerator README](https://github.com/mganss/XmlSchemaClassGenerator), [JAXB RI releases](https://github.com/eclipse-ee4j/jaxb-ri/releases) | Existing evidence: `SchemaResolverSecurityTest`, `JdkXmlAdaptersTest`, `XpXsd10DocumentConformanceTest`, and the selected native conformance secure-resource checks cover resolver and runtime-jdkxml denial paths. `TASK-0073` should add a path-by-path threat-model review and any missing parser-path denial tests. |
| Naming collisions and customization | [jaxb-tools releases](https://github.com/highsource/jaxb-tools/releases), [XmlSchemaClassGenerator README](https://github.com/mganss/XmlSchemaClassGenerator) | Existing evidence: `BindingModelBuilderTest`, `CoreGeneratorTest`, `MxjbCliTest`, and `MxjbGradlePluginFunctionalTest` cover deterministic naming and package mapping basics. `TASK-0075` should add focused collision fixtures for duplicate names across namespaces, Java keywords, nested anonymous types, generated branch names, and package mapping failures. |
| Setup/tooling confusion | [XmlSchemaClassGenerator issue 256](https://github.com/mganss/XmlSchemaClassGenerator/issues/256), [jaxb-tools releases](https://github.com/highsource/jaxb-tools/releases) | Existing evidence: `MxjbCliTest`, `MxjbGradlePluginFunctionalTest`, and `publicationDryRun` cover current CLI, Gradle, and staged artifact paths. `TASK-0072` should validate release-asset consumption from a clean downstream build; `TASK-0074` should improve missing-path and misconfiguration diagnostics. |
| Generated API ergonomics | [XmlSchemaClassGenerator README](https://github.com/mganss/XmlSchemaClassGenerator), [xsdata issue 564](https://github.com/tefra/xsdata/issues/564) | Existing evidence: `docs/architecture/generated-code-contract.md`, `GeneratedModelEmitterTest`, `GeneratedReaderEmitterTest`, and `GeneratedWriterEmitterTest` cover explicit record/sealed branch contracts for accepted profiles. Ergonomics should be reviewed in `TASK-0074` and `TASK-0077` documentation simplification. |
| AOT/Native Image behavior | [jaxb-tools issue 204](https://github.com/highsource/jaxb-tools/issues/204), [jaxb-tools releases](https://github.com/highsource/jaxb-tools/releases), [JAXB RI releases](https://github.com/eclipse-ee4j/jaxb-ri/releases) | Existing evidence: `GeneratorCoreArchitectureTest`, `RuntimeArchitectureTest`, `GeneratorGradlePluginArchitectureTest`, `nativeSmoke`, and `nativeConformance` cover no-reflection policy and Native Image execution. `TASK-0076` should re-run and document Native Image sustainability after full-profile enablement. |

## Follow-on candidates

- Add W3C generated-binding rows for choice/content-model, wildcard, derivation, identity, datatype,
  and include/import shapes that are already within accepted product scope.
- Add naming-collision fixtures for Java keywords, duplicate local names across namespaces, branch
  generated names, and user package mappings.
- Add diagnostics tests for missing schema paths, denied resources, invalid local repository paths,
  unsupported customization requests, and unmapped W3C suite paths.
- Add a Native Image sustainability review that checks reflection/proxy absence, resource handling,
  SDKMAN GraalVM setup, and known warning text.

No broad external suite was vendored, and no product behavior was expanded by this task.
