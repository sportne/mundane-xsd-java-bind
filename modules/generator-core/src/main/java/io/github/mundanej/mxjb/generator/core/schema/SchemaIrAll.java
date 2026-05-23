package io.github.mundanej.mxjb.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized XSD 1.0 xs:all particle with unordered singleton element members. */
public record SchemaIrAll(SchemaCardinality cardinality, List<SchemaIrElement> elements)
    implements SchemaIrParticle {
  public SchemaIrAll {
    Objects.requireNonNull(cardinality, "cardinality");
    elements = List.copyOf(elements);
  }

  @Override
  public String toText(String indent) {
    String line = indent + "all cardinality=" + cardinality.toText();
    if (elements.isEmpty()) {
      return line;
    }
    return line
        + "\n"
        + elements.stream()
            .map(element -> element.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
  }
}
