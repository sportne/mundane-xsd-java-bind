package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized XSD simpleContent value metadata. */
public record SchemaIrSimpleContent(
    SchemaIrTypeReference valueType, SchemaIrSimpleRestriction restriction) {
  public SchemaIrSimpleContent(SchemaIrTypeReference valueType) {
    this(valueType, null);
  }

  public SchemaIrSimpleContent {
    Objects.requireNonNull(valueType, "valueType");
  }

  public String toText(String indent) {
    return indent
        + "simpleContent type="
        + valueType.toText()
        + (restriction == null ? "" : " restriction " + restriction.toText());
  }
}
