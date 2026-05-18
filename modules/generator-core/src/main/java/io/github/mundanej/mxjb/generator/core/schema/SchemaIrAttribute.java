package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized attribute declaration or use. */
public record SchemaIrAttribute(
    SchemaQName name,
    SchemaIrTypeReference type,
    String use,
    SchemaIrValueSemantics semantics,
    boolean reference) {
  public SchemaIrAttribute(
      SchemaQName name, SchemaIrTypeReference type, String use, boolean reference) {
    this(name, type, use, SchemaIrValueSemantics.NONE, reference);
  }

  public SchemaIrAttribute {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    use = use == null || use.isBlank() ? "optional" : use;
    semantics = semantics == null ? SchemaIrValueSemantics.NONE : semantics;
  }

  public String toText(String indent) {
    String prefix = reference ? "attributeRef " : "attribute ";
    return indent
        + prefix
        + name.toText()
        + " type="
        + type.toText()
        + " use="
        + use
        + semantics.toText();
  }
}
