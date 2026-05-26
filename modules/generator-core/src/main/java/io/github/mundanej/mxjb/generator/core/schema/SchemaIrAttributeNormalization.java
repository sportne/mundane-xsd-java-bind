package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Map;

/** Package-private attribute namespace and value-semantics normalization helpers. */
final class SchemaIrAttributeNormalization {
  private SchemaIrAttributeNormalization() {}

  static SchemaQName name(
      String targetNamespace,
      Map<String, String> schemaAttributes,
      Map<String, String> attributeNodeAttributes,
      String localName,
      boolean global) {
    if (global) {
      return new SchemaQName(targetNamespace, localName);
    }
    String form = attributeNodeAttributes.get("form");
    boolean qualified =
        "qualified".equals(form)
            || (form == null && "qualified".equals(schemaAttributes.get("attributeFormDefault")));
    return new SchemaQName(qualified ? targetNamespace : "", localName);
  }

  static SchemaIrValueSemantics semantics(Map<String, String> attributes) {
    return new SchemaIrValueSemantics(
        "true".equals(attributes.get("nillable")),
        attributes.get("default"),
        attributes.get("fixed"));
  }
}
