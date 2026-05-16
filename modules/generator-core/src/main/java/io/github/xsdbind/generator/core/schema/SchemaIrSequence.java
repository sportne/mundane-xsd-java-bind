package io.github.xsdbind.generator.core.schema;

import java.util.List;
import java.util.Objects;

/** Normalized ordered sequence particle. */
public record SchemaIrSequence(SchemaCardinality cardinality, List<SchemaIrElement> elements) {
  public SchemaIrSequence {
    Objects.requireNonNull(cardinality, "cardinality");
    elements = List.copyOf(elements);
  }

  public String toText(String indent) {
    String line = indent + "sequence cardinality=" + cardinality.toText();
    if (elements.isEmpty()) {
      return line;
    }
    return line + "\n" + SchemaIrElement.elementsText(elements, indent + "  ");
  }
}
