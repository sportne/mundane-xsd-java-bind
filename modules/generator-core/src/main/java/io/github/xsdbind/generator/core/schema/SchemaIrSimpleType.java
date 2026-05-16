package io.github.xsdbind.generator.core.schema;

import java.util.Objects;

/** Normalized placeholder for a named simple type before facet support exists. */
public record SchemaIrSimpleType(SchemaQName name) {
  public SchemaIrSimpleType {
    Objects.requireNonNull(name, "name");
  }

  public String toText(String indent) {
    return indent + "simpleType " + name.toText();
  }
}
