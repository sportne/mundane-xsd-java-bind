package io.github.mundanej.mxjb.generator.core.schema;

import java.util.Objects;

/** Accepted named simple-type list metadata in normalized IR. */
public record SchemaIrSimpleList(SchemaQName itemType) {
  public SchemaIrSimpleList {
    Objects.requireNonNull(itemType, "itemType");
  }

  public String toText() {
    return "itemType=" + itemType.toText();
  }
}
