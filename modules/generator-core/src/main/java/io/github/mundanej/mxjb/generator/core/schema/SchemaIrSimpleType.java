package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Normalized named simple type. */
public record SchemaIrSimpleType(
    SchemaQName name,
    SchemaIrSimpleRestriction restriction,
    SchemaIrSimpleList list,
    SchemaIrSimpleUnion union) {
  public SchemaIrSimpleType(SchemaQName name, SchemaIrSimpleRestriction restriction) {
    this(name, restriction, null, null);
  }

  public SchemaIrSimpleType {
    Objects.requireNonNull(name, "name");
    int shapeCount = 0;
    if (restriction != null) {
      shapeCount++;
    }
    if (list != null) {
      shapeCount++;
    }
    if (union != null) {
      shapeCount++;
    }
    if (shapeCount > 1) {
      throw new IllegalArgumentException("simple type can have only one accepted shape");
    }
  }

  public String toText(String indent) {
    return indent
        + "simpleType "
        + name.toText()
        + (restriction == null ? "" : " restriction " + restriction.toText())
        + (list == null ? "" : " list " + list.toText())
        + (union == null ? "" : " union " + union.toText());
  }
}
