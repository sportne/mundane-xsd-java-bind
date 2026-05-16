package io.github.xsdbind.generator.core.schema;

import java.util.Objects;

/** Normalized attribute declaration or use. */
public record SchemaIrAttribute(
    SchemaQName name, SchemaIrTypeReference type, String use, boolean reference) {
  public SchemaIrAttribute {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    use = use == null || use.isBlank() ? "optional" : use;
  }

  public String toText(String indent) {
    String prefix = reference ? "attributeRef " : "attribute ";
    return indent + prefix + name.toText() + " type=" + type.toText() + " use=" + use;
  }
}
