package io.github.mundanej.mxjb.generator.core.schema;

/** Accepted named simple-type list metadata in normalized IR. */
public record SchemaIrSimpleList(SchemaQName itemType, SchemaIrSimpleRestriction itemRestriction) {
  public SchemaIrSimpleList(SchemaQName itemType) {
    this(itemType, null);
  }

  public SchemaIrSimpleList {
    if (itemType == null && itemRestriction == null) {
      throw new IllegalArgumentException("list item type is required");
    }
    if (itemType != null && itemRestriction != null) {
      throw new IllegalArgumentException("list item type must be referenced or anonymous");
    }
  }

  public String toText() {
    return itemType == null
        ? "itemType=anonymous[" + itemRestriction.toText() + "]"
        : "itemType=" + itemType.toText();
  }
}
