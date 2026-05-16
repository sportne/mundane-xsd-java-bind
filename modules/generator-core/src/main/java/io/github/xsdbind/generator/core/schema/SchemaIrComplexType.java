package io.github.xsdbind.generator.core.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Normalized complex type with attributes and ordered sequences. */
public record SchemaIrComplexType(
    SchemaQName name,
    List<SchemaIrAttribute> attributes,
    List<SchemaIrSequence> sequences,
    boolean anonymous) {
  public SchemaIrComplexType {
    if (!anonymous) {
      Objects.requireNonNull(name, "name");
    }
    attributes = List.copyOf(attributes);
    sequences = List.copyOf(sequences);
  }

  public String toText(String indent) {
    String line = indent + "complexType " + (anonymous ? "anonymous" : name.toText());
    String sequenceText =
        sequences.stream()
            .map(sequence -> sequence.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
    String attributeText =
        attributes.stream()
            .map(attribute -> attribute.toText(indent + "  "))
            .collect(Collectors.joining("\n"));
    return java.util.stream.Stream.of(line, sequenceText, attributeText)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.joining("\n"));
  }
}
