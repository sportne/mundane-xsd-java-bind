package io.github.mundanej.mxjb.generator.core.schema;

/** Normalized occurrence constraints for supported particles. */
public record SchemaCardinality(int minOccurs, String maxOccurs) {
  public static final SchemaCardinality ONE = new SchemaCardinality(1, "1");

  public SchemaCardinality {
    if (maxOccurs == null || maxOccurs.isBlank()) {
      maxOccurs = "1";
    }
  }

  public String toText() {
    return minOccurs + ".." + maxOccurs;
  }
}
