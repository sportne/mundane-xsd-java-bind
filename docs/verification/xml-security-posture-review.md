# XML security posture review

`TASK-0073` reviewed the XML parsing and validation paths after `1.0.0`. The review keeps the
published support scope unchanged and treats secure defaults as evidence requirements, not new
product claims.

| Path | Default posture | Evidence |
|---|---|---|
| Schema resolver include/import intake | Resolver-approved local files and explicit catalog mappings only; HTTP/HTTPS primary schemas and unmapped network references remain denied. StAX parsing disables DTD and external entities. | `SchemaResolverSecurityTest`; `REQ-RES-001`; `REQ-SEC-001` |
| XSD syntax frontend | Consumes only resolver-approved `ResolvedSchema` paths. StAX parsing disables DTD and external entities before reading schema syntax. | `XsdSyntaxParser` frontend tests plus resolver security tests |
| Runtime JDK XML adapter | `JdkXmlAdapters.secureInputFactory()` disables DTD and external entities and installs a denying XML resolver. Generated readers and XML validators consume the project `XmlEventReader` abstraction rather than opening resources directly. | `JdkXmlAdaptersTest`; native conformance resolver-denial case |
| Generated readers and validators | Generated code has no file, network, DOM, or JAXP factory ownership. Security depends on the caller-provided `XmlEventReader`; project examples, conformance lanes, benchmarks, and release consumer smoke use the secure JDK adapter. | generated-source tests; selected conformance fixtures; `releaseConsumerSmoke` |
| Retained wildcard fragments | Retained `XmlFragment` and `XmlAttribute` values are immutable data captured from the caller-controlled event stream. They do not retain parser handles and do not perform resource resolution during validation or writing. | `TASK-0037`, `TASK-0039`, `TASK-0062` evidence |
| W3C suite metadata intake | DOM metadata parsing disables XInclude, entity expansion, DOCTYPE declarations, external general/parameter entities, and enables secure processing. | `W3cXsd10SuiteIntakeTest.rejectsDoctypeInSuiteMetadata` |
| W3C generated-binding JDK schema oracle | The opt-in W3C generated-binding execution path now uses a hardened `SchemaFactory` with `ACCESS_EXTERNAL_DTD` and `ACCESS_EXTERNAL_SCHEMA` set to empty strings before validating mapped rows. | `W3cXsd10SuiteIntakeTest.secureSchemaFactoryRejectsExternalSchemaAccess` |
| Native Image paths | Native confidence is provided by the SDKMAN GraalVM execution of `nativeSmoke` and `nativeConformance`, including the selected external-entity/resource denial case. | `nativeSmoke`; `nativeConformance` |

Unsupported behavior remains explicit: no DTD/entity identity preservation, no default network
fetching, no DOM-backed generated binding, no XML 1.1/XSD 1.1 support, and no lexical-prefix or
canonical XML claims.
