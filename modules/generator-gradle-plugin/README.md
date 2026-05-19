# generator-gradle-plugin

Gradle integration for schema generation.

## Plugin id

```groovy
plugins {
    id 'java'
    id 'io.github.mundanej.mxjb'
}
```

The plugin registers an `mxjb` extension and a `generateMxjbSources` task. When the Java plugin is
present, `build/generated/sources/mxjb/java` is added to `sourceSets.main.java`, and `compileJava`
depends on generation.

## Configuration

```groovy
mxjb {
    schema('src/main/resources/schema/order.xsd')
    localRoot('src/main/resources/schema')
    catalog('https://example.invalid/common.xsd', 'src/main/resources/schema/common.xsd')
    namespacePackage('urn:orders', 'com.example.orders')
    namespacePackage('urn:lines', 'com.example.lines')

    profile = 'XP-DATA-10'
    defaultPackage = 'com.example.generated'
    outputDirectory = layout.buildDirectory.dir('generated/sources/mxjb/java')
}
```

Supported properties and methods:

- `schema(Object)` or `schemas.from(...)`: explicit primary schema inputs.
- `outputDirectory`: generated Java source directory; defaults to
  `build/generated/sources/mxjb/java`.
- `profile`: public generator profile token; currently `XP-DATA-10`,
  opt-in `XP-DATA-10-CHOICE`, opt-in `XP-VALIDATION-10-BASIC`, or opt-in
  `XP-XSD10-COMPOSED`, opt-in `XP-XSD10-SEMANTIC`, or opt-in `XP-XSD10-DOCUMENT`.
  The composed profile covers the accepted named group and attribute-group, list/union
  simple type, and initial derivation subsets only; the semantic profile adds accepted
  nillable/default/fixed, direct substitution-group, and generated semantic validation behavior;
  the document profile adds accepted direct `xs:any` wildcard/open-content retention, accepted
  mixed-content sequence models, and stable project XML serialization policy evidence.
- `defaultPackage`: fallback package for namespaces without an explicit mapping.
- `namespacePackage(String, String)`: namespace-to-Java-package mapping.
- `localRoot(Object)`: local schema root used by the resolver and declared as a task input.
- `catalog(String, Object)`: URI-to-local-file mapping used by the resolver.

Generation diagnostics fail the task and are printed as stable manifest lines:

```text
code | resource | message
```

## Build behavior

Schema resolution and source generation run only inside the task action. The task declares schema,
local-root, catalog, scalar configuration, and output-directory inputs/outputs and is compatible
with Gradle's configuration cache.

The task generates into a temporary directory first. On success, it replaces its owned output
directory so stale generated sources are removed. On failure, it does not copy partially generated
sources into the configured output directory.

## Contributor notes

- Keep task inputs and outputs configuration-cache compatible.
- Do not resolve schemas during configuration.
- Do not add external CLI parsing or Gradle helper dependencies without dependency review.
