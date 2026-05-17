package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized named simple type. */
public record SchemaIrSimpleType(SchemaQName name, SchemaIrSimpleRestriction restriction) {
  public SchemaIrSimpleType {
    Objects.requireNonNull(name, "name");
  }

  public String toText(String indent) {
    return indent
        + "simpleType "
        + name.toText()
        + (restriction == null ? "" : " restriction " + restriction.toText());
  }
}
